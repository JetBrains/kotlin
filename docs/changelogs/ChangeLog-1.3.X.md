# Changelog 1.3.X

## 1.3.72 - IDE plugins update

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
- [`KT-34203`](https://youtrack.jetbrains.com/issue/KT-34203) 'Add constructor parameter' fix does not add generics
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


## 1.3.61

### Compiler

- [`KT-35004`](https://youtrack.jetbrains.com/issue/KT-35004) "AssertionError: Unsigned type expected" in `when` range check in extension on unsigned type

### IDE

- [`KT-34923`](https://youtrack.jetbrains.com/issue/KT-34923) [Regression] KtUltraLightMethod.hasModifierProperty("native") returns false for external Kotlin functions


### Libraries

- [`KT-21445`](https://youtrack.jetbrains.com/issue/KT-21445) W3C DOM Touch events and interfaces are incomplete / missing

### Tools. Compiler Plugins

- [`KT-34991`](https://youtrack.jetbrains.com/issue/KT-34991) kotlinx.serialization: False warning "Explicit @Serializable annotation on enum class is required when @SerialName or @SerialInfo annotations are used on its members"

### Tools. J2K

- [`KT-34987`](https://youtrack.jetbrains.com/issue/KT-34987) New J2K converter: @NotNull annotations are not removed after converting java code to kotlin
- [`KT-35074`](https://youtrack.jetbrains.com/issue/KT-35074) J2K: No auto conversion in 'for' loop with multiple init variables

## 1.3.60

### Android

- [`KT-27170`](https://youtrack.jetbrains.com/issue/KT-27170) Android lint tasks fails in Gradle with MPP dependency

### Compiler

#### New Features

- [`KT-31230`](https://youtrack.jetbrains.com/issue/KT-31230) Refine rules for allowed Array-based class literals on different platforms: allow `Array::class` everywhere, disallow `Array<...>::class` on non-JVM
- [`KT-33413`](https://youtrack.jetbrains.com/issue/KT-33413) Allow 'break' and 'continue' in 'when' statement to point to innermost surrounding loop

#### Performance Improvements

- [`KT-14513`](https://youtrack.jetbrains.com/issue/KT-14513) Suboptimal compilation of lazy delegated properties with inline getValue
- [`KT-28507`](https://youtrack.jetbrains.com/issue/KT-28507) Extra InlineMarker.mark invocation in generated suspending function bytecode
- [`KT-29229`](https://youtrack.jetbrains.com/issue/KT-29229) Intrinsify 'in' operator for unsigned integer ranges

#### Fixes

- [`KT-7354`](https://youtrack.jetbrains.com/issue/KT-7354) Confusing error message when trying to access package local java class
- [`KT-9310`](https://youtrack.jetbrains.com/issue/KT-9310) Don't make interface and DefaultImpls methods synchronized
- [`KT-11430`](https://youtrack.jetbrains.com/issue/KT-11430) Improve diagnostics for dangling lambdas
- [`KT-16526`](https://youtrack.jetbrains.com/issue/KT-16526) Provide better error explanation when one tries to delegate var to read-only delegate
- [`KT-20258`](https://youtrack.jetbrains.com/issue/KT-20258) Improve annotation rendering in diagnostic messages
- [`KT-22275`](https://youtrack.jetbrains.com/issue/KT-22275) Unify exceptions from null checks
- [`KT-27503`](https://youtrack.jetbrains.com/issue/KT-27503) Private functions uses from inside of suspendCoroutine go though accessor
- [`KT-28938`](https://youtrack.jetbrains.com/issue/KT-28938) Coroutines tail-call optimization does not work for generic returns that had instantiated to Unit
- [`KT-29385`](https://youtrack.jetbrains.com/issue/KT-29385) "AnalyzerException: Expected an object reference, but found I" for EXACTLY_ONCE non-inline contract with captured class constructor parameter
- [`KT-29510`](https://youtrack.jetbrains.com/issue/KT-29510) "RuntimeException: Trying to access skipped parameter" with EXACTLY_ONCE contract and nested call of crossinline lambda
- [`KT-29614`](https://youtrack.jetbrains.com/issue/KT-29614) java.lang.VerifyError: Bad type on operand stack  - in inlining,  crossinline in constructor with EXACTLY_ONCE contract
- [`KT-30275`](https://youtrack.jetbrains.com/issue/KT-30275) Get rid of session in FirElement
- [`KT-30744`](https://youtrack.jetbrains.com/issue/KT-30744) Invoking Interface Static Method from Extension method generates incorrect jvm bytecode
- [`KT-30785`](https://youtrack.jetbrains.com/issue/KT-30785) Equality comparison of inline classes results in boxing
- [`KT-32217`](https://youtrack.jetbrains.com/issue/KT-32217) FIR: support delegated properties resolve
- [`KT-32433`](https://youtrack.jetbrains.com/issue/KT-32433) NI: UninferredParameterTypeConstructor with class property
- [`KT-32587`](https://youtrack.jetbrains.com/issue/KT-32587) NI: Type mismatch "String" vs "String" in IDE on generic .invoke on generic delegated property
- [`KT-32689`](https://youtrack.jetbrains.com/issue/KT-32689) Shuffled line numbers in suspend functions with elvis operator
- [`KT-32851`](https://youtrack.jetbrains.com/issue/KT-32851) Constraint for callable reference argument doesn't take into account use-site variance
- [`KT-32864`](https://youtrack.jetbrains.com/issue/KT-32864) The line number of assertFailsWith in suspending function is lost
- [`KT-33125`](https://youtrack.jetbrains.com/issue/KT-33125) NI: "Rewrite at slice INDEXED_LVALUE_SET" with Mutable Map set index operator inside "@kotlin.BuilderInference" block
- [`KT-33414`](https://youtrack.jetbrains.com/issue/KT-33414) 'java.lang.AssertionError: int type expected, but null was found in basic frames' in kotlin-io while building library train
- [`KT-33421`](https://youtrack.jetbrains.com/issue/KT-33421) Please make NOTHING_TO_INLINE warning shorter
- [`KT-33504`](https://youtrack.jetbrains.com/issue/KT-33504) EA-209823 - ISE: ProjectResolutionFacade$computeModuleResolverProvider$resolverForProject$$.invoke: Can't find builtIns by key CacheKeyBySdk
- [`KT-33572`](https://youtrack.jetbrains.com/issue/KT-33572) Scripting import with implicit receiver doesn't work
- [`KT-33821`](https://youtrack.jetbrains.com/issue/KT-33821) Compiler should not rely on the default locale when generating boxing for suspend functions
- [`KT-18541`](https://youtrack.jetbrains.com/issue/KT-18541) Prohibit "tailrec" modifier on open functions
- [`KT-19844`](https://youtrack.jetbrains.com/issue/KT-19844) Do not render type annotations on symbols rendered in diagnostic messages
- [`KT-24913`](https://youtrack.jetbrains.com/issue/KT-24913) KotlinFrontEndException with local class in init of generic class
- [`KT-28940`](https://youtrack.jetbrains.com/issue/KT-28940) Concurrency issue for lazy values with the post-computation phase
- [`KT-31540`](https://youtrack.jetbrains.com/issue/KT-31540) Change initialization order of default values for tail recursive optimized functions

### Docs & Examples

- [`KT-26212`](https://youtrack.jetbrains.com/issue/KT-26212) Update docs to explicitly mention that union is opposite of intersect
- [`KT-34086`](https://youtrack.jetbrains.com/issue/KT-34086) Website, stdlib api docs: unresolved link jvm/stdlib/kotlin.text/-charsets/Charset

### IDE

#### Fixes

- [`KT-8581`](https://youtrack.jetbrains.com/issue/KT-8581) 'Move Statement' doesn't work for statement finished by semicolon
- [`KT-9204`](https://youtrack.jetbrains.com/issue/KT-9204) Shorten references and some other IDE features have problem when package name clash with class name
- [`KT-17993`](https://youtrack.jetbrains.com/issue/KT-17993) Annotations are colored the same as language keywords
- [`KT-21037`](https://youtrack.jetbrains.com/issue/KT-21037) LazyLightClassMemberMatchingError$WrongMatch “Matched :BAR MemberIndex(index=0) to :BAR MemberIndex(index=1) in KtLightClassImpl” after duplicating values inside enum class
- [`KT-23305`](https://youtrack.jetbrains.com/issue/KT-23305) We should be able to see platform-specific errors in common module
- [`KT-23461`](https://youtrack.jetbrains.com/issue/KT-23461) `Move statement up/down` attaches a comment block to the function being moved
- [`KT-26960`](https://youtrack.jetbrains.com/issue/KT-26960) IDE doesn't report `actual` without `expect` placed into a custom platform-agnostic source set
- [`KT-27243`](https://youtrack.jetbrains.com/issue/KT-27243) LazyLightClassMemberMatchingError when overriding hidden member
- [`KT-28404`](https://youtrack.jetbrains.com/issue/KT-28404) Gradle configuration page is missing from a New Project Wizard creation flow for multiplatform templates
- [`KT-30824`](https://youtrack.jetbrains.com/issue/KT-30824) No highlighting of declaration/usage of function with functional-type (lambda) parameter on its usage
- [`KT-31117`](https://youtrack.jetbrains.com/issue/KT-31117) AssertionError at `CompletionBindingContextProvider._getBindingContext` when typing any character within string with injected Kotlin
- [`KT-31139`](https://youtrack.jetbrains.com/issue/KT-31139) "Override members" on enum inserts semicolon before enum body
- [`KT-31810`](https://youtrack.jetbrains.com/issue/KT-31810) Paste inside indented `.trimIndent()` raw string doesn't respect indentation
- [`KT-32401`](https://youtrack.jetbrains.com/issue/KT-32401) Exceptions while running IDEA in headless mode for building searchable options
- [`KT-32543`](https://youtrack.jetbrains.com/issue/KT-32543) UltraLight support for Kotlin collections.
- [`KT-32544`](https://youtrack.jetbrains.com/issue/KT-32544) Support UltraLight classes for local/anonymous/enum classes
- [`KT-32799`](https://youtrack.jetbrains.com/issue/KT-32799) 2019.2 RC (192.5728.74) Kotlin plugin exception during build searchable options (Directory index may not be queried for default project)
- [`KT-33008`](https://youtrack.jetbrains.com/issue/KT-33008) IDEA does not report in MPP: Upper bound of a type parameter cannot be an array
- [`KT-33316`](https://youtrack.jetbrains.com/issue/KT-33316) Kotlin Facet: make sure the order of allPlatforms value is fixed
- [`KT-33561`](https://youtrack.jetbrains.com/issue/KT-33561) LazyLightClassMemberMatchingError when overloading synthetic member
- [`KT-33584`](https://youtrack.jetbrains.com/issue/KT-33584) Make kotlin light classes return no-arg constructor when no-arg (or jpa) compiler plugin is enabled
- [`KT-33775`](https://youtrack.jetbrains.com/issue/KT-33775) please remove usages of org.intellij.plugins.intelliLang.inject.InjectorUtils#putInjectedFileUserData(com.intellij.lang.injection.MultiHostRegistrar, com.intellij.openapi.util.Key<T>, T) deprecated eons ago
- [`KT-33813`](https://youtrack.jetbrains.com/issue/KT-33813) Poor formatting of 'Selected target platforms' and 'Depends on' in facet settings
- [`KT-33937`](https://youtrack.jetbrains.com/issue/KT-33937) delay() completion from kotlinx.coroutines causes happening of root package in code
- [`KT-33973`](https://youtrack.jetbrains.com/issue/KT-33973) Kotlin objects could abuse idea plugin functionality
- [`KT-34000`](https://youtrack.jetbrains.com/issue/KT-34000) Import quickfix does not work for extension methods from objects
- [`KT-34070`](https://youtrack.jetbrains.com/issue/KT-34070) "No target platforms selected" message for commonTest facet at mobile shared library project
- [`KT-34191`](https://youtrack.jetbrains.com/issue/KT-34191) Since-build .. until-build compatibility ranges are the same for 192 and 193 IDE plugins
- [`KT-21153`](https://youtrack.jetbrains.com/issue/KT-21153) IDE: string template + annotation usage: ISE: "Couldn't get delegate" at LightClassDataHolderKt.findDelegate()
- [`KT-33352`](https://youtrack.jetbrains.com/issue/KT-33352) "KotlinExceptionWithAttachments: Couldn't get delegate for class" on nested class/object
- [`KT-34042`](https://youtrack.jetbrains.com/issue/KT-34042) "Error loading Kotlin facets. Kotlin facets are not allowed in Kotlin/Native Module" in 192 IDEA
- [`KT-34237`](https://youtrack.jetbrains.com/issue/KT-34237) MPP with Android target: `common*` source sets are not shown as source roots in IDE
- [`KT-33626`](https://youtrack.jetbrains.com/issue/KT-33626) Deadlock with Kotlin LockBasedStorageManager in IDEA commit dialog
- [`KT-34402`](https://youtrack.jetbrains.com/issue/KT-34402) Unresolved reference to Kotlin.test library in CommonTest in Multiplatform project without JVM target
- [`KT-34639`](https://youtrack.jetbrains.com/issue/KT-34639) Multiplatform project with the only (Android) target is incorrectly imported into IDE

### IDE. Completion

- [`KT-10340`](https://youtrack.jetbrains.com/issue/KT-10340) Import completion unable to shorten fq-names when there is a conflict between package name and local identifier
- [`KT-17689`](https://youtrack.jetbrains.com/issue/KT-17689) Code completion for enum typealias doesn't show members
- [`KT-28998`](https://youtrack.jetbrains.com/issue/KT-28998) Slow completion for build.gradle.kts (Kotlin Gradle DSL script)
- [`KT-30996`](https://youtrack.jetbrains.com/issue/KT-30996) DSL extension methods which are not applicable are offered for completion
- [`KT-31902`](https://youtrack.jetbrains.com/issue/KT-31902) Fully qualified name is used for `delay` instead of  import and just method name
- [`KT-33903`](https://youtrack.jetbrains.com/issue/KT-33903) Duplicating completion for imported extensions from companion objects

### IDE. Debugger

- [`KT-10984`](https://youtrack.jetbrains.com/issue/KT-10984) Disallow placing line breakpoints without executable code (changed)
- [`KT-22116`](https://youtrack.jetbrains.com/issue/KT-22116) Support function breakpoints
- [`KT-24408`](https://youtrack.jetbrains.com/issue/KT-24408) @InlineOnly: Misleading status for breakpoints in inline functions
- [`KT-27645`](https://youtrack.jetbrains.com/issue/KT-27645) Debugger breakpoints do not work in suspend function executed in SpringBoot controller (MVC and WebFlux)
- [`KT-32687`](https://youtrack.jetbrains.com/issue/KT-32687) Disallow breakpoints for @InlineOnly function bodies
- [`KT-32813`](https://youtrack.jetbrains.com/issue/KT-32813) Exception on invoking "Smart Step Into"
- [`KT-32830`](https://youtrack.jetbrains.com/issue/KT-32830) NPE on changing class property in Evaluate Expression window
- [`KT-33064`](https://youtrack.jetbrains.com/issue/KT-33064) “Read access is allowed from event dispatch thread or inside read-action only” from KotlinLineBreakpointType.createLineSourcePosition on adding new line before the current one while stopping on breakpoint
- [`KT-11395`](https://youtrack.jetbrains.com/issue/KT-11395) Breakpoint inside lambda argument of InlineOnly function doesn't work

### IDE. Folding

- [`KT-6314`](https://youtrack.jetbrains.com/issue/KT-6314) Folding of "when" construction

### IDE. Gradle

- [`KT-33038`](https://youtrack.jetbrains.com/issue/KT-33038) Package prefix is not imported in non-MPP project
- [`KT-33987`](https://youtrack.jetbrains.com/issue/KT-33987) Serialization exception during importing Kotlin project in IDEA 192
- [`KT-32960`](https://youtrack.jetbrains.com/issue/KT-32960) KotlinMPPGradleModelBuilder takes a long time to process when syncing non-MPP project with IDE
- [`KT-34424`](https://youtrack.jetbrains.com/issue/KT-34424) With Kotlin plugin in Gradle project without Native the IDE fails to start Gradle task: "Kotlin/Native properties file is absent at null/konan/konan.properties"
- [`KT-34256`](https://youtrack.jetbrains.com/issue/KT-34256) Fail to use multiplatform modules with dependsOn with android plugin
- [`KT-34663`](https://youtrack.jetbrains.com/issue/KT-34663) Low performance of MPP 1.2 during import with module-per-source-set enabled

### IDE. Gradle. Script

- [`KT-31766`](https://youtrack.jetbrains.com/issue/KT-31766) Gradle Kotlin DSL new project template: use type-safe model accessors
- [`KT-34463`](https://youtrack.jetbrains.com/issue/KT-34463) New Gradle-based project template misses pluginManagement{} block in EAP branch
- [`KT-31767`](https://youtrack.jetbrains.com/issue/KT-31767) Gradle Kotlin DSL new project template: use settings.gradle.kts

### IDE. Inspections and Intentions

#### New Features

- [`KT-26431`](https://youtrack.jetbrains.com/issue/KT-26431) Quickfix to remove redundant label
- [`KT-28049`](https://youtrack.jetbrains.com/issue/KT-28049) Suggest import quickfix for operator extension functions
- [`KT-29622`](https://youtrack.jetbrains.com/issue/KT-29622) "Move to separate file" intention should also work for sealed class
- [`KT-33178`](https://youtrack.jetbrains.com/issue/KT-33178) Use a new compiler flag -Xinline-classes during enabling the feature via IDEA intention
- [`KT-33586`](https://youtrack.jetbrains.com/issue/KT-33586) "Constructors are not allowed for objects" diagnostic needs quickfix to change object to class

#### Fixes

- [`KT-12291`](https://youtrack.jetbrains.com/issue/KT-12291) Override/Implement Members: better member positioning inside the class
- [`KT-14899`](https://youtrack.jetbrains.com/issue/KT-14899) Quickfix "Create member function" inserts too many semicolons when applied to Enum
- [`KT-15700`](https://youtrack.jetbrains.com/issue/KT-15700) "Convert lambda to reference" does not work with backtick-escaped references
- [`KT-18772`](https://youtrack.jetbrains.com/issue/KT-18772) "Introduce subject to when": don't choose an object or a constant as the subject
- [`KT-21172`](https://youtrack.jetbrains.com/issue/KT-21172) Join declaration and assignment should place the result at the assignment, not at declaration
- [`KT-25697`](https://youtrack.jetbrains.com/issue/KT-25697) `Replace with dot call` quickfix breaks formatting
- [`KT-26635`](https://youtrack.jetbrains.com/issue/KT-26635) An empty line is added after `actual` modifier on "Create actual annotation class..." quick fix applied to annotation if it is annotated with comment
- [`KT-27270`](https://youtrack.jetbrains.com/issue/KT-27270) "Add jar to classpath" quick fix modifies build.gradle of MPP project in a way that fails to be imported
- [`KT-28471`](https://youtrack.jetbrains.com/issue/KT-28471) "Add initializer" quickfix initializes non-null variable with null
- [`KT-28538`](https://youtrack.jetbrains.com/issue/KT-28538) `create expected ...` quick fix illegally creates `expect` member with a usage of a platform-specific type
- [`KT-28549`](https://youtrack.jetbrains.com/issue/KT-28549) Create actual/expect quick fix for class/object doesn't add import for an inherited member
- [`KT-28620`](https://youtrack.jetbrains.com/issue/KT-28620) `Create expect/actual ...` quick fix could save @Test annotation on generation
- [`KT-28740`](https://youtrack.jetbrains.com/issue/KT-28740) AE “2 declarations in var bar: [ERROR : No type, no body]” after applying “Create actual class” quick fix for class with property which has not specified type
- [`KT-28947`](https://youtrack.jetbrains.com/issue/KT-28947) Backing field has created after applying “Create expected class in common module...” intention
- [`KT-30136`](https://youtrack.jetbrains.com/issue/KT-30136) False negative "Redundant explicit 'this'" with local variable
- [`KT-30794`](https://youtrack.jetbrains.com/issue/KT-30794) Quickfix for unchecked cast produces invalid code
- [`KT-31133`](https://youtrack.jetbrains.com/issue/KT-31133) Liveness analysis on enum does not take into account calls to 'values'
- [`KT-31433`](https://youtrack.jetbrains.com/issue/KT-31433) Incorrect "Create expected class..." for class with supertype
- [`KT-31475`](https://youtrack.jetbrains.com/issue/KT-31475) "Create expect..." should delete 'override' modifier
- [`KT-31587`](https://youtrack.jetbrains.com/issue/KT-31587) Redundant `private` modifier before primary constructor after create `actual` class
- [`KT-31921`](https://youtrack.jetbrains.com/issue/KT-31921) "Create expected ..."/"Create actual..." quick fix: `val` and `vararg` modifiers are misordered in the generated `expect`/`actual` declaration
- [`KT-31999`](https://youtrack.jetbrains.com/issue/KT-31999) "Variable declaration could be moved into `when`" inspection suggests to inline expression containing return (throw) statement
- [`KT-32012`](https://youtrack.jetbrains.com/issue/KT-32012) Change parameter type quick fix: Don't use qualified name
- [`KT-32468`](https://youtrack.jetbrains.com/issue/KT-32468) False positive SimplifiableCall "filter call could be simplified to filterIsInstance" with expression body function and explicit return type
- [`KT-32479`](https://youtrack.jetbrains.com/issue/KT-32479) False positive "Redundant overriding method" with derived property and base function starting with `get`, `set` or `is` (Accidental override)
- [`KT-32571`](https://youtrack.jetbrains.com/issue/KT-32571) "Create expect" quick fix incorrectly treats multiplatform stdlib typealiased types as platform-specific ones
- [`KT-32580`](https://youtrack.jetbrains.com/issue/KT-32580) "Remove braces" QF for single-expression function with inferred lambda return type: "ClassCastException: class kotlin.reflect.jvm.internal.KClassImpl cannot be cast to class kotlin.jvm.internal.ClassBasedDeclarationContainer"
- [`KT-32582`](https://youtrack.jetbrains.com/issue/KT-32582) Ambiguous message for [AMBIGUOUS_ACTUALS] error (master)
- [`KT-32586`](https://youtrack.jetbrains.com/issue/KT-32586) "Make member open" quick fix doesn't update all the related actualisations of an expected member
- [`KT-32616`](https://youtrack.jetbrains.com/issue/KT-32616) "To ordinary string literal" doesn't remove indents, newlines and `trimIndent`
- [`KT-32642`](https://youtrack.jetbrains.com/issue/KT-32642) "Create expect" quick fix doesn't warn about a platform-specific annotation applied to the generated member
- [`KT-32650`](https://youtrack.jetbrains.com/issue/KT-32650) "Replace 'if' with 'when'" removes braces from 'if' statement
- [`KT-32694`](https://youtrack.jetbrains.com/issue/KT-32694) "Create expect"/"create actual" quick fix doesn't transfer use-site annotations
- [`KT-32737`](https://youtrack.jetbrains.com/issue/KT-32737) "Create expect" quick fix adds `actual` modifier to an interface function with default implementation without a warning
- [`KT-32768`](https://youtrack.jetbrains.com/issue/KT-32768) "Create expect" quick fix doesn't warn about a local supertype of an `actual` class while generating an expected declaration
- [`KT-32829`](https://youtrack.jetbrains.com/issue/KT-32829) "Add .jar to the classpath" quick fix creates "compile"/"testCompile" dependencies in build.gradle
- [`KT-32972`](https://youtrack.jetbrains.com/issue/KT-32972) No "remove braces" inspection for ${this}
- [`KT-32981`](https://youtrack.jetbrains.com/issue/KT-32981) "Create enum constant" quick fix adds redundant empty line
- [`KT-33060`](https://youtrack.jetbrains.com/issue/KT-33060) "Cleanup code" does not remove 'final' keyword for overridden function with non-canonical modifiers order
- [`KT-33115`](https://youtrack.jetbrains.com/issue/KT-33115) "Replace overloaded operator with function call" intention should not be shown on incomplete expressions
- [`KT-33150`](https://youtrack.jetbrains.com/issue/KT-33150) Don't suggest create expect function from function with `private` modifier
- [`KT-33153`](https://youtrack.jetbrains.com/issue/KT-33153) False positive "Redundant overriding method" when overriding package private method
- [`KT-33204`](https://youtrack.jetbrains.com/issue/KT-33204) False positive "flatMap call could be simplified to flatten()" with Array
- [`KT-33299`](https://youtrack.jetbrains.com/issue/KT-33299) "Create type parameter from usage" should work with backticks
- [`KT-33300`](https://youtrack.jetbrains.com/issue/KT-33300) "Create type parameter from usage" suggests for top level property
- [`KT-33302`](https://youtrack.jetbrains.com/issue/KT-33302) KNPE after "Create type parameter from usage" with typealias
- [`KT-33357`](https://youtrack.jetbrains.com/issue/KT-33357) 'java.lang.Throwable: Assertion failed: Refactorings should be invoked inside transaction 'exception occurs when extracting sealed class from file with the same name
- [`KT-33362`](https://youtrack.jetbrains.com/issue/KT-33362) Inspection "Extract class from current file" is not available for 'sealed' keyword
- [`KT-33437`](https://youtrack.jetbrains.com/issue/KT-33437) “Argument rangeInElement (0,1) endOffset must not exceed descriptor text range (0, 0) length (0).” on creating Kotlin Script files inside package
- [`KT-33612`](https://youtrack.jetbrains.com/issue/KT-33612) "Replace with safe call" quick fix moves code to another line
- [`KT-33660`](https://youtrack.jetbrains.com/issue/KT-33660) "Convert to anonymous object" with nested SAM interface inserts `object` keyword in the wrong place
- [`KT-33718`](https://youtrack.jetbrains.com/issue/KT-33718) "Create enum constant" quick fix adds after semicolon
- [`KT-33754`](https://youtrack.jetbrains.com/issue/KT-33754) Improve error hint message for "Create expect/actual..."
- [`KT-33880`](https://youtrack.jetbrains.com/issue/KT-33880) "Convert to range check" produces code that is subject to ReplaceRangeToWithUntil for range with exclusive upper bound
- [`KT-33930`](https://youtrack.jetbrains.com/issue/KT-33930) Don't suggest "create expect" quick fix on `lateinit` and `const` top-level properties
- [`KT-33981`](https://youtrack.jetbrains.com/issue/KT-33981) “KotlinCodeInsightWorkspaceSettings is registered as application service, but requested as project one” on opening QF menu for some fixes in IJ193
- [`KT-32965`](https://youtrack.jetbrains.com/issue/KT-32965) False positive "Redundant qualifier name" with nested enum member call
- [`KT-33597`](https://youtrack.jetbrains.com/issue/KT-33597) False positive "Redundant qualifier name" with class property initialized with same-named object property
- [`KT-33991`](https://youtrack.jetbrains.com/issue/KT-33991) False positive "Redundant qualifier name" with enum member function call
- [`KT-34113`](https://youtrack.jetbrains.com/issue/KT-34113) False positive "Redundant qualifier name" with Enum.values() from a different Enum

### IDE. KDoc

- [`KT-20777`](https://youtrack.jetbrains.com/issue/KT-20777) KDoc: Type parameters are not shown in sample code

### IDE. Multiplatform

- [`KT-26333`](https://youtrack.jetbrains.com/issue/KT-26333) IDE incorrectly requires `actual` implementations to be present in all the project source sets
- [`KT-28537`](https://youtrack.jetbrains.com/issue/KT-28537) Platform-specific type taken from a dependency module isn't reported in `common` code
- [`KT-32562`](https://youtrack.jetbrains.com/issue/KT-32562) Provide a registry key to enable/disable hierarchical multiplatform mechanism in IDE

### IDE. Navigation

- [`KT-28075`](https://youtrack.jetbrains.com/issue/KT-28075) Duplicate "implements" gutter icons on some interfaces
- [`KT-30052`](https://youtrack.jetbrains.com/issue/KT-30052) Duplicated "is subclassed" editor gutter icons
- [`KT-33182`](https://youtrack.jetbrains.com/issue/KT-33182) com.intellij.idea.IdeStarter#main has four (!) icons, should be two

### IDE. REPL

- [`KT-33329`](https://youtrack.jetbrains.com/issue/KT-33329) IllegalArgumentException in REPL

### IDE. Refactorings

- [`KT-24929`](https://youtrack.jetbrains.com/issue/KT-24929) 'Search for references' checkbox state isn't saved on move of kotlin file
- [`KT-30342`](https://youtrack.jetbrains.com/issue/KT-30342) Move refactoring: suggest file name starting with an uppercase letter
- [`KT-32426`](https://youtrack.jetbrains.com/issue/KT-32426) Invalid code format after "Pull Members Up" on function with comment and another indent
- [`KT-32496`](https://youtrack.jetbrains.com/issue/KT-32496) "Problems Detected" dialog message about conflicting declarations on moving file to another package is absolutely unreadable
- [`KT-33059`](https://youtrack.jetbrains.com/issue/KT-33059) Exception [Assertion failed: Write access is allowed inside write-action only] in case of Move class to nonexistent folder
- [`KT-33972`](https://youtrack.jetbrains.com/issue/KT-33972) Change signature should affect all hierarchy

### IDE. Run Configurations

- [`KT-34366`](https://youtrack.jetbrains.com/issue/KT-34366) Implement gutters for running tests (multi-platform projects)

### IDE. Scratch

- [`KT-23986`](https://youtrack.jetbrains.com/issue/KT-23986) No access to stdout output in Kotlin scratch
- [`KT-23989`](https://youtrack.jetbrains.com/issue/KT-23989) Scratch: allow copy of a scratch output
- [`KT-28910`](https://youtrack.jetbrains.com/issue/KT-28910) Add hint for Make before Run checkbox
- [`KT-29407`](https://youtrack.jetbrains.com/issue/KT-29407) strange output for long strings
- [`KT-31295`](https://youtrack.jetbrains.com/issue/KT-31295) Kotlin worksheet in projects, not as scratch files
- [`KT-32366`](https://youtrack.jetbrains.com/issue/KT-32366) Sidebar as alternative output layout
- [`KT-33585`](https://youtrack.jetbrains.com/issue/KT-33585) Synchronized highlighting of the main editor and side panel

### IDE. Script

- [`KT-30206`](https://youtrack.jetbrains.com/issue/KT-30206) Settings / ... / Kotlin Scripting with no project opened causes ISE: "project.baseDir must not be null" at ScriptTemplatesFromDependenciesProvider.loadScriptDefinitions()
- [`KT-32513`](https://youtrack.jetbrains.com/issue/KT-32513) Intellij hangs in ApplicationUtilsKt.runWriteAction through ScriptDependenciesLoader$submitMakeRootsChange$doNotifyRootsChanged$1.run

### IDE. Wizards

- [`KT-27587`](https://youtrack.jetbrains.com/issue/KT-27587) Bump Android build tools version at `Multiplatform (Android/iOS)` template of the New Project Wizard
- [`KT-33927`](https://youtrack.jetbrains.com/issue/KT-33927) MPP, Kotlin New project wizard: broken project generation
- [`KT-34108`](https://youtrack.jetbrains.com/issue/KT-34108) Gradle Kotlin DSL: generated project with `tasks` element fails on configuration stage with Gradle 4.10
- [`KT-34154`](https://youtrack.jetbrains.com/issue/KT-34154) New Project wizard: build.gradle.kts: type-safe code sets JVM 1.8 for main, but JVM 1.6 for test
- [`KT-34229`](https://youtrack.jetbrains.com/issue/KT-34229) New Project wizard: IDEA 193+: Mobile Android/iOS: creating another project of this type tries to write into previous one

### JavaScript

- [`KT-12935`](https://youtrack.jetbrains.com/issue/KT-12935) Generated source maps for JS mention nonexistent dummy.kt
- [`KT-26701`](https://youtrack.jetbrains.com/issue/KT-26701) JS, rollup.js: Application can't depend on a library if both sourcemaps reference "dummy.kt"

### Libraries

- [`KT-26309`](https://youtrack.jetbrains.com/issue/KT-26309) Avoid division in string-to-number conversions
- [`KT-27545`](https://youtrack.jetbrains.com/issue/KT-27545) File.copyTo: unclear error message when it fails to delete the destination
- [`KT-28804`](https://youtrack.jetbrains.com/issue/KT-28804) Wrong parameter name in kotlin.text.contentEquals
- [`KT-32024`](https://youtrack.jetbrains.com/issue/KT-32024) Modify `Iterable<T>.take(n)` implementation not to call `.next()` more than necessary
- [`KT-32532`](https://youtrack.jetbrains.com/issue/KT-32532) MutableList<T>.removeAll is lacking documentation
- [`KT-32728`](https://youtrack.jetbrains.com/issue/KT-32728) CollectionsKt.windowed throws IllegalArgumentException (Illegal Capacity: -1) when size param is Integer.MAX_VALUE due to overflow operation
- [`KT-33864`](https://youtrack.jetbrains.com/issue/KT-33864) Read from pseudo-file system is empty

### Reflection

- [`KT-13936`](https://youtrack.jetbrains.com/issue/KT-13936) KotlinReflectionInternalError on invoking callBy on overridden member with inherited default argument value
- [`KT-17860`](https://youtrack.jetbrains.com/issue/KT-17860) Improve KParameter.toString for receiver parameters

### Tools

- [`KT-17045`](https://youtrack.jetbrains.com/issue/KT-17045) Drop MaxPermSize support from compiler daemon
- [`KT-32259`](https://youtrack.jetbrains.com/issue/KT-32259) `org.jetbrains.annotations` module exported from embeddable compiler, causes problems in Java modular builds

### Tools. Android Extensions

- [`KT-32096`](https://youtrack.jetbrains.com/issue/KT-32096) IDE plugin doesn't recognize that Parcelize is no longer experimental

### Tools. CLI

- [`KT-24991`](https://youtrack.jetbrains.com/issue/KT-24991) CLI: Empty classpath in `kotlin` script except for `kotlin-runner.jar`
- [`KT-26624`](https://youtrack.jetbrains.com/issue/KT-26624) Set Thread.contextClassLoader when running programs with 'kotlin' launcher script or scripts with 'kotlinc -script'
- [`KT-24966`](https://youtrack.jetbrains.com/issue/KT-24966) Classloader problems when running basic kafka example with `kotlin` and `kotlinc`

### Tools. Compiler Plugins

- [`KT-29471`](https://youtrack.jetbrains.com/issue/KT-29471) output from jvm-api-gen plugin on classpath crashes downstream kotlinc-jvm: inline method with inner class
- [`KT-33630`](https://youtrack.jetbrains.com/issue/KT-33630) cannot use @kotlinx.serialization.Transient and lateinit together on 1.3.50

### Tools. Daemon

- [`KT-32992`](https://youtrack.jetbrains.com/issue/KT-32992) Enable assertions in Kotlin Compile Daemon
- [`KT-33027`](https://youtrack.jetbrains.com/issue/KT-33027) Compilation with daemon fails, because IncrementalModuleInfo#serialVersionUID does not match

### Tools. Gradle

#### New Features

- [`KT-20760`](https://youtrack.jetbrains.com/issue/KT-20760) Kotlin Gradle Plugin doesn't allow for configuring friend paths through API
- [`KT-34009`](https://youtrack.jetbrains.com/issue/KT-34009) Associate compilations in the target–compilation project model

#### Performance Improvements

- [`KT-31666`](https://youtrack.jetbrains.com/issue/KT-31666) Kotlin plugin configures all tasks in a project when `kotlin.incremental` is enabled

#### Fixes

- [`KT-17630`](https://youtrack.jetbrains.com/issue/KT-17630) User test Gradle source set code cannot reach out internal members from the production code
- [`KT-22213`](https://youtrack.jetbrains.com/issue/KT-22213) Android Extensions experimental mode doesn't work with Gradle Kotlin DSL
- [`KT-31077`](https://youtrack.jetbrains.com/issue/KT-31077) android.kotlinOptions block is lacking its type
- [`KT-31641`](https://youtrack.jetbrains.com/issue/KT-31641) Kapt configurations miss attributes to resolve MPP dependencies: Cannot choose between the following variants ...
- [`KT-31713`](https://youtrack.jetbrains.com/issue/KT-31713) ConcurrentModificationException: Realize Pending during execution phase
- [`KT-32678`](https://youtrack.jetbrains.com/issue/KT-32678) Bugfixes in HMPP source set visibility
- [`KT-32679`](https://youtrack.jetbrains.com/issue/KT-32679) Testing & test tasks API in the target–compilation model
- [`KT-32804`](https://youtrack.jetbrains.com/issue/KT-32804) Kapt-generated Java sources in jvm+withJava MPP module are not compiled and bundled
- [`KT-32853`](https://youtrack.jetbrains.com/issue/KT-32853) ConcurrentModificationException when compiling with Gradle.
- [`KT-32872`](https://youtrack.jetbrains.com/issue/KT-32872) Gradle test runner for Native does not show failed build if process quit without starting printing results.
- [`KT-33105`](https://youtrack.jetbrains.com/issue/KT-33105) kapt+withJava in multiplatform module depending on other multiplatform fails on 1.3.50-eap-54
- [`KT-33469`](https://youtrack.jetbrains.com/issue/KT-33469) Drop support for Gradle versions older than 4.3 in the Kotlin Gradle plugin
- [`KT-33470`](https://youtrack.jetbrains.com/issue/KT-33470) Drop support for Gradle versions older than 4.9 in the Kotlin Gradle plugin
- [`KT-33980`](https://youtrack.jetbrains.com/issue/KT-33980) Read the granular source sets metadata flag value once and cache it for the current Gradle build
- [`KT-34312`](https://youtrack.jetbrains.com/issue/KT-34312) UnsupportedOperationException on `requiresVisibilityOf` in the Kotlin Gradle plugin

### Tools. Gradle. JS

#### New Features

- [`KT-31478`](https://youtrack.jetbrains.com/issue/KT-31478) Gradle, JS tests, Karma: Support sourcemaps in Gradle stacktraces
- [`KT-32073`](https://youtrack.jetbrains.com/issue/KT-32073) Gradle, JS, karma: parse errors and warnings from karma output
- [`KT-32075`](https://youtrack.jetbrains.com/issue/KT-32075) Gradle, JS, karma: download chrome headless using puppeteer

#### Fixes

- [`KT-31663`](https://youtrack.jetbrains.com/issue/KT-31663) Gradle/JS: with not installed browser specified for browser test the response is "Successful, 0 tests found"
- [`KT-32216`](https://youtrack.jetbrains.com/issue/KT-32216) Gradle, JS, tests: filter doesn't work
- [`KT-32224`](https://youtrack.jetbrains.com/issue/KT-32224) In Gradle Kotlin/JS projects, the `browserWebpack` task does not rerun when the `main` compilation's outputs change
- [`KT-32281`](https://youtrack.jetbrains.com/issue/KT-32281) Gradle, JS, karma: Headless chrome output is not captured
- [`KT-33288`](https://youtrack.jetbrains.com/issue/KT-33288) JS: Incorrect bundle with webpack output.library and source maps
- [`KT-33313`](https://youtrack.jetbrains.com/issue/KT-33313) When a Kotlin/JS test task runs using a custom compilation, it doesn't track the compilation outputs in its up-to-date checks
- [`KT-33547`](https://youtrack.jetbrains.com/issue/KT-33547) Template JS Client and JVM Server works wrong on 1.3.50 Kotlin
- [`KT-33549`](https://youtrack.jetbrains.com/issue/KT-33549) Gradle Kotlin/JS external declarations: search for `typings` key inside `package.json`
- [`KT-33579`](https://youtrack.jetbrains.com/issue/KT-33579) Js tests with mocha cannot be run
- [`KT-33710`](https://youtrack.jetbrains.com/issue/KT-33710) Task "generateExternals" for automatic Dukat execution does not work
- [`KT-33716`](https://youtrack.jetbrains.com/issue/KT-33716) Gradle, Yarn: yarn is not downloading via YarnSetupTask
- [`KT-34101`](https://youtrack.jetbrains.com/issue/KT-34101) CCE class org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest_Decorated cannot be cast to class org.gradle.api.provider.Provider on importing Gradle project with JS
- [`KT-34123`](https://youtrack.jetbrains.com/issue/KT-34123) "Cannot find node module "kotlin-test-js-runner/kotlin-test-karma-runner.js"" in `JS Client and JVM Server` new project wizard template
- [`KT-32319`](https://youtrack.jetbrains.com/issue/KT-32319) Gradle, js, webpack: source-map-loader failed to load source contents from relative urls
- [`KT-33417`](https://youtrack.jetbrains.com/issue/KT-33417) NodeTest failed with error "Failed to create MD5 hash" after NodeRun is executed
- [`KT-33747`](https://youtrack.jetbrains.com/issue/KT-33747) Exception doesn't fail the test in kotlin js node runner
- [`KT-33828`](https://youtrack.jetbrains.com/issue/KT-33828) jsPackageJson task fails after changing artifact origin repository
- [`KT-34460`](https://youtrack.jetbrains.com/issue/KT-34460) NPM packages clash if declared in dependencies and devDependencies both
- [`KT-34555`](https://youtrack.jetbrains.com/issue/KT-34555) [Kotlin/JS] Unsafe webpack config merge

### Tools. Gradle. Native

- [`KT-33076`](https://youtrack.jetbrains.com/issue/KT-33076) MPP Gradle plugin: Produce final native binaries from compilation output instead of sources
- [`KT-33645`](https://youtrack.jetbrains.com/issue/KT-33645) Kotlin/Native: Compilation failure if a library passed by the -Xinclude option contains a constructor annotated with @OverrideInit
- [`KT-34259`](https://youtrack.jetbrains.com/issue/KT-34259) MPP Gradle plugin: Support fat frameworks for watchOS and tvOS
- [`KT-34329`](https://youtrack.jetbrains.com/issue/KT-34329) Support watchOS and tvOS in CocoaPods Gradle plugin

### Tools. J2K

#### New Features

- [`KT-7940`](https://youtrack.jetbrains.com/issue/KT-7940) J2K: convert Integer.MAX_VALUE to Int.MAX_VALUE
- [`KT-22412`](https://youtrack.jetbrains.com/issue/KT-22412) J2K: Intention to replace if(...) throw IAE with require
- [`KT-22680`](https://youtrack.jetbrains.com/issue/KT-22680) Request: when converting Java->Kotlin, try to avoid creating functions for constant fields (`static final`)

#### Performance Improvements

- [`KT-33725`](https://youtrack.jetbrains.com/issue/KT-33725) Java->Kotlin converter on paste performs expensive reparse in unrelated contexts
- [`KT-33854`](https://youtrack.jetbrains.com/issue/KT-33854) J2K conversion of Interface freezes UI for more than 10 seconds without progress dialog
- [`KT-33875`](https://youtrack.jetbrains.com/issue/KT-33875) [NewJ2K] InspectionLikeProcessingGroup pipeline rework: query isApplicable in parallel for all element first, apply relevant after in EDT

#### Fixes

- [`KT-19603`](https://youtrack.jetbrains.com/issue/KT-19603) A mutable container property updated from another class converts to red code
- [`KT-19607`](https://youtrack.jetbrains.com/issue/KT-19607) Static member qualified by child class converted to red code
- [`KT-20035`](https://youtrack.jetbrains.com/issue/KT-20035) Automatic conversion from Java 1.8 to Kotlin 1.1.4 using Idea 2017.2.2: null!!
- [`KT-21504`](https://youtrack.jetbrains.com/issue/KT-21504) J2K: Convert Long.parseLong(s) to s.toLong()
- [`KT-24293`](https://youtrack.jetbrains.com/issue/KT-24293) Bug: conversion of Java "List" into Kotlin doesn't produce "MutableList"
- [`KT-32253`](https://youtrack.jetbrains.com/issue/KT-32253) Converting Java class with field initialized by constructor parameter used to initialize a different field or named as a different field produces red code
- [`KT-32696`](https://youtrack.jetbrains.com/issue/KT-32696) New J2K: java List is wrongly converted when pasting it to Kotlin file
- [`KT-32903`](https://youtrack.jetbrains.com/issue/KT-32903) J2K: Static import is converted to unresolved reference
- [`KT-33235`](https://youtrack.jetbrains.com/issue/KT-33235) Remove "Replace guard clause with kotlin's function call" inspection and tranform it to J2K post-processing
- [`KT-33434`](https://youtrack.jetbrains.com/issue/KT-33434) UninitializedPropertyAccessException occurs after J2K convertion of package with custom functional interface and it's usage
- [`KT-33445`](https://youtrack.jetbrains.com/issue/KT-33445) Two definitions of org.jetbrains.kotlin.idea.j2k.J2kPostProcessing in Kotlin 1.3.50-rc
- [`KT-33500`](https://youtrack.jetbrains.com/issue/KT-33500) Unresolved reference after J2K convertion of isNaN/isFinite
- [`KT-33556`](https://youtrack.jetbrains.com/issue/KT-33556) J2K converter fails on statically imported global overloaded functions
- [`KT-33679`](https://youtrack.jetbrains.com/issue/KT-33679) Result of assignment with operation differs in kotlin after J2K conversion
- [`KT-33687`](https://youtrack.jetbrains.com/issue/KT-33687) Extra empty lines are added after comment after J2K conversion
- [`KT-33743`](https://youtrack.jetbrains.com/issue/KT-33743) Reference to static field outside its class is unresolved after J2K conversion
- [`KT-33756`](https://youtrack.jetbrains.com/issue/KT-33756) J2K: main method with varargs is converted to non-runnable main kotlin method
- [`KT-33863`](https://youtrack.jetbrains.com/issue/KT-33863) java.lang.IllegalStateException: argument must not be null exception occurs on J2K conversion of Generic class usage without type parameter
- [`KT-19355`](https://youtrack.jetbrains.com/issue/KT-19355) "Variable expected" error after J2K for increment/decrement of an object field
- [`KT-19569`](https://youtrack.jetbrains.com/issue/KT-19569) Java wrappers for primitives are converted to nullable types with nullability errors in Kotlin
- [`KT-30643`](https://youtrack.jetbrains.com/issue/KT-30643) J2K: wrong position of TYPE_USE annotation
- [`KT-32518`](https://youtrack.jetbrains.com/issue/KT-32518) Nullability information is lost after J2K convertion of constructor with null parameter
- [`KT-33941`](https://youtrack.jetbrains.com/issue/KT-33941) J2K: Overload resolution ambiguity with assertThat and `StackOverflowError` in IDEA
- [`KT-33942`](https://youtrack.jetbrains.com/issue/KT-33942) New J2K: `StackOverflowError` from `org.jetbrains.kotlin.nj2k.inference.common.BoundTypeCalculatorImpl.boundTypeUnenhanced`
- [`KT-34164`](https://youtrack.jetbrains.com/issue/KT-34164) J2K: on converting static method references in other .java sources are not corrected
- [`KT-34165`](https://youtrack.jetbrains.com/issue/KT-34165) J2K: imports are lost in conversion, references resolve to different same-named classes
- [`KT-34266`](https://youtrack.jetbrains.com/issue/KT-34266) Multiple errors after converting Java class implementing an interface from another file

### Tools. JPS

- [`KT-33808`](https://youtrack.jetbrains.com/issue/KT-33808) JPS compilation is not incremental in IDEA 2019.3

### Tools. Maven

- [`KT-34006`](https://youtrack.jetbrains.com/issue/KT-34006) Maven plugin do not consider .kts files as Kotlin sources
- [`KT-34011`](https://youtrack.jetbrains.com/issue/KT-34011) Kotlin scripting plugin is not loaded by default from kotlin maven plugin

### Tools. REPL

- [`KT-27956`](https://youtrack.jetbrains.com/issue/KT-27956) REPL/Script: extract classes and names right from ClassLoader

### Tools. Scripts

- [`KT-31661`](https://youtrack.jetbrains.com/issue/KT-31661) ClassNotFoundException in runtime for 'kotlinc -script' while compilation is fine
- [`KT-31704`](https://youtrack.jetbrains.com/issue/KT-31704) [kotlin-scripting] passing `name` to String.toScriptSource make script compilation failed
- [`KT-32234`](https://youtrack.jetbrains.com/issue/KT-32234) "Unable to derive module descriptor" when using Kotlin compiler (embeddable) in Java 9+ modular builds
- [`KT-33529`](https://youtrack.jetbrains.com/issue/KT-33529) NCDF running kotlin script from command line
- [`KT-33554`](https://youtrack.jetbrains.com/issue/KT-33554) Classpath not passed properly when evaluating standard script with `kotlinc`
- [`KT-33892`](https://youtrack.jetbrains.com/issue/KT-33892) REPL/Script: Implement mechanism for resolve top-level functions and properties from classloader
- [`KT-34294`](https://youtrack.jetbrains.com/issue/KT-34294) SamWithReceiver cannot be used with new scripting API

### Tools. kapt

- [`KT-31291`](https://youtrack.jetbrains.com/issue/KT-31291) Incremental Kapt: IllegalArgumentException from `org.jetbrains.org.objectweb.asm.ClassVisitor.<init>`
- [`KT-33028`](https://youtrack.jetbrains.com/issue/KT-33028) Kapt error "Unable to find package java.lang in classpath or bootclasspath" on JDK 11 with `-source 8`
- [`KT-33050`](https://youtrack.jetbrains.com/issue/KT-33050) kapt does not honor source/target compatibility of enclosing project
- [`KT-33052`](https://youtrack.jetbrains.com/issue/KT-33052) Kapt generates invalid java stubs for enum members with class bodies on JDK 11
- [`KT-33056`](https://youtrack.jetbrains.com/issue/KT-33056) Incremental kapt is disabled due to `javaslang.match.PatternsProcessor` processor on classpath when Worker API is enabled
- [`KT-33493`](https://youtrack.jetbrains.com/issue/KT-33493) 1.3.50, org.jetbrains.org.objectweb.asm.ClassVisitor.<init>
- [`KT-33515`](https://youtrack.jetbrains.com/issue/KT-33515) Incremental kapt fails when I remove an annotated file
- [`KT-33889`](https://youtrack.jetbrains.com/issue/KT-33889) Incremental KAPT: NoSuchMethodError: 'java.util.regex.Pattern com.sun.tools.javac.processing.JavacProcessingEnvironment.validImportStringToPattern(java.lang.String)'
- [`KT-33503`](https://youtrack.jetbrains.com/issue/KT-33503) Kapt, Spring Boot: "Could not resolve all files for configuration ':_classStructurekaptKotlin'"
- [`KT-33800`](https://youtrack.jetbrains.com/issue/KT-33800) KAPT aptMode=compile fails to compile certain legitimate code

## 1.3.50

### Compiler

- [`KT-12787`](https://youtrack.jetbrains.com/issue/KT-12787) Debugger: Generate line number at end of function (to set a breakpoint on the last line of the block)
- [`KT-23675`](https://youtrack.jetbrains.com/issue/KT-23675) "Parameter specified as non-null is null: method org.jetbrains.kotlin.codegen.FrameMapBase.getIndex, parameter descriptor" when classes are defined inside an anonymous extension function and access a field of the extension function's `this` instance
- [`KT-24596`](https://youtrack.jetbrains.com/issue/KT-24596) Refactor / Inline const property does not insert its value into usage in annotation
- [`KT-25497`](https://youtrack.jetbrains.com/issue/KT-25497) kotlinx.serialization - throws Backend Internal error exception during code generation of sealed classes
- [`KT-28927`](https://youtrack.jetbrains.com/issue/KT-28927) "IllegalStateException: Arrays of class literals are not supported yet" in AnnotationDeserializer.resolveArrayElementType
- [`KT-31070`](https://youtrack.jetbrains.com/issue/KT-31070) IndexOutOfBoundsException in Analyzer with @JvmOverloads constructor with 34+ parameters
- [`KT-31265`](https://youtrack.jetbrains.com/issue/KT-31265) FIR: experimental compiler
- [`KT-31535`](https://youtrack.jetbrains.com/issue/KT-31535) False positives from compiler warning IMPLICIT_NOTHING_AS_TYPE_PARAMETER
- [`KT-31969`](https://youtrack.jetbrains.com/issue/KT-31969) NI: false positive USELESS_ELVIS with multiple elvis calls
- [`KT-32044`](https://youtrack.jetbrains.com/issue/KT-32044) For loop over full UByte range terminates at UInt bound.
- [`KT-25432`](https://youtrack.jetbrains.com/issue/KT-25432) No smartcast on qualifier expression of captured type
- [`KT-30796`](https://youtrack.jetbrains.com/issue/KT-30796) psi2ir generates IrErrorType for elvis with generic type having nullable upper-bound when expected type is not nullable
- [`KT-31242`](https://youtrack.jetbrains.com/issue/KT-31242) "Can't find enclosing method" proguard compilation exception with inline and crossinline
- [`KT-31347`](https://youtrack.jetbrains.com/issue/KT-31347) "IndexOutOfBoundsException: Insufficient maximum stack size" with crossinline and suspend
- [`KT-31367`](https://youtrack.jetbrains.com/issue/KT-31367) IllegalStateException: Concrete fake override public open fun (...)  defined in TheIssue[PropertyGetterDescriptorImpl@1a03c376] should have exactly one concrete super-declaration: []
- [`KT-31734`](https://youtrack.jetbrains.com/issue/KT-31734) Empty parameter list required on Annotations of function types
- [`KT-32434`](https://youtrack.jetbrains.com/issue/KT-32434) New type inference fails for Caffeine Cache
- [`KT-32452`](https://youtrack.jetbrains.com/issue/KT-32452) Kotlin 1.3.40 - problem in IDE with new type inference and suspending method reference
- [`KT-32407`](https://youtrack.jetbrains.com/issue/KT-32407) NI: "use property access syntax" intention causes freezes in editor
- [`KT-33127`](https://youtrack.jetbrains.com/issue/KT-33127) Script result value is not calculated properly for the last expression
- [`KT-33157`](https://youtrack.jetbrains.com/issue/KT-33157) Inline class with generic method is considered bad class by javac

### Docs & Examples

- [`KT-16602`](https://youtrack.jetbrains.com/issue/KT-16602) Provide examples of sorting API usage
- [`KT-32353`](https://youtrack.jetbrains.com/issue/KT-32353) Document order of array elements initialization

### IDE

#### New Features

- [`KT-28098`](https://youtrack.jetbrains.com/issue/KT-28098) Insert space after automatically closed right brace of nested lambda to follow code style

#### Fixes

- [`KT-16476`](https://youtrack.jetbrains.com/issue/KT-16476) Extend selection (Select Word) doesn't select just KDoc if cursor is just before the KDoc
- [`KT-21374`](https://youtrack.jetbrains.com/issue/KT-21374) Imports optimized tooltip is displayed, even if no changes were made
- [`KT-21422`](https://youtrack.jetbrains.com/issue/KT-21422) IDE can't import class from root package
- [`KT-27344`](https://youtrack.jetbrains.com/issue/KT-27344) MPP: jvmWithJava: no IDE module dependency is created between Kotlin test and Java main on import; Gradle build is successful
- [`KT-29667`](https://youtrack.jetbrains.com/issue/KT-29667) Kotlin update settings has wrong looking text boxes for versions
- [`KT-30133`](https://youtrack.jetbrains.com/issue/KT-30133) Update copyright creates duplicates for build.gradle.kts files
- [`KT-30782`](https://youtrack.jetbrains.com/issue/KT-30782) 'Show Method Separators' does not separate expression body Kotlin functions
- [`KT-31022`](https://youtrack.jetbrains.com/issue/KT-31022) `Quick definition` does not show Kotlin code in Java files
- [`KT-31499`](https://youtrack.jetbrains.com/issue/KT-31499) "Extend selection" selects escaped identifier name together with backticks
- [`KT-31595`](https://youtrack.jetbrains.com/issue/KT-31595) "Complete current statement" for method call closes brace at wrong place
- [`KT-31637`](https://youtrack.jetbrains.com/issue/KT-31637) NPE in IDE when organizing imports
- [`KT-31786`](https://youtrack.jetbrains.com/issue/KT-31786) KNPE at copy attempt due to kdoc reference
- [`KT-32276`](https://youtrack.jetbrains.com/issue/KT-32276) Fix flaky test for ultra light classes
- [`KT-32364`](https://youtrack.jetbrains.com/issue/KT-32364) Remove deprecated usages of OUT_OF_CODE_BLOCK_MODIFICATION_COUNT and write a replacement for Kotlin language
- [`KT-32370`](https://youtrack.jetbrains.com/issue/KT-32370) Lambdas should have implicit `return` in Kotlin Uast
- [`KT-12096`](https://youtrack.jetbrains.com/issue/KT-12096) Spring: rename of Kotlin bean defined in `@Bean` annotation fails
- [`KT-28193`](https://youtrack.jetbrains.com/issue/KT-28193) Exception: Mirror element should never be calculated for light classes generated from a single file
- [`KT-28822`](https://youtrack.jetbrains.com/issue/KT-28822) Dependencies in Kotlin MPP project could be wrongly resolved if project was not build before import
- [`KT-29267`](https://youtrack.jetbrains.com/issue/KT-29267) Enable ultra-light classes by default
- [`KT-31129`](https://youtrack.jetbrains.com/issue/KT-31129) Call only Kotlin-specific reference contributors for getting Kotlin references from PSI
- [`KT-32082`](https://youtrack.jetbrains.com/issue/KT-32082) Kotlin facet: 1.3.40 plugin does not properly read target platform settings of 1.3.50 plugin
- [`KT-32969`](https://youtrack.jetbrains.com/issue/KT-32969) Data class extending abstract class with final `toString`, `equals` or `hashCode` causes exception
- [`KT-33245`](https://youtrack.jetbrains.com/issue/KT-33245) IllegalArgumentException exception occurs on Tools->Configure Koltin in Project action in Android Studio

### IDE. Completion

- [`KT-9792`](https://youtrack.jetbrains.com/issue/KT-9792) Don't propose the same name for arguments of lambda on completion of function call with lambda template
- [`KT-29572`](https://youtrack.jetbrains.com/issue/KT-29572) Smart completing anonymous object uses incorrect code style
- [`KT-25264`](https://youtrack.jetbrains.com/issue/KT-25264) Freeze in Kotlin file on completion
- [`KT-32519`](https://youtrack.jetbrains.com/issue/KT-32519) Keyword completion: support fixing layout and typo tolerance

### IDE. Debugger

#### New Features

- [`KT-30740`](https://youtrack.jetbrains.com/issue/KT-30740) Display more information about variables when breakpoint is set inside lambda expression

#### Fixes

- [`KT-8579`](https://youtrack.jetbrains.com/issue/KT-8579) Debugger: Evaluate expression fails at typed arrays
- [`KT-10183`](https://youtrack.jetbrains.com/issue/KT-10183) Debugger: receiver properties are not shown inline in extension function
- [`KT-11663`](https://youtrack.jetbrains.com/issue/KT-11663) Assignment is not possible in Evaluate expression
- [`KT-11706`](https://youtrack.jetbrains.com/issue/KT-11706) Attempts to evaluate java method calls on 'Array' instance in debugger fail with NoSuchMethodError
- [`KT-11888`](https://youtrack.jetbrains.com/issue/KT-11888) Evaluate Expression for expression with synchronized
- [`KT-11938`](https://youtrack.jetbrains.com/issue/KT-11938) Empty condition is marked as error
- [`KT-13188`](https://youtrack.jetbrains.com/issue/KT-13188) Cannot evaluate expression with local extension function
- [`KT-14421`](https://youtrack.jetbrains.com/issue/KT-14421) Debugger: breakpoint set on trivial if/while is not hit
- [`KT-15259`](https://youtrack.jetbrains.com/issue/KT-15259) Debug: closing brace of object definition is considered executable; ISE: "Don't call this method for local declarations: OBJECT_DECLARATION" at LazyDeclarationResolver.getMemberScopeDeclaredIn()
- [`KT-19084`](https://youtrack.jetbrains.com/issue/KT-19084) Breakpoints on Debugger altering Result
- [`KT-19556`](https://youtrack.jetbrains.com/issue/KT-19556) Kotlin exception while debugging IJ plugin code
- [`KT-19980`](https://youtrack.jetbrains.com/issue/KT-19980) Debug: evaluation fails for setter of member extention property
- [`KT-20560`](https://youtrack.jetbrains.com/issue/KT-20560) Evaluate expression doesn't work for super method call
- [`KT-23526`](https://youtrack.jetbrains.com/issue/KT-23526) In *.kts scripts, debugger ignores breakpoints in top-level statements and members
- [`KT-24914`](https://youtrack.jetbrains.com/issue/KT-24914) AS: Uninitialized yet lazy properties called on first debug point reach
- [`KT-26742`](https://youtrack.jetbrains.com/issue/KT-26742) Debugger can't evaluate expected top-level function from common code
- [`KT-30120`](https://youtrack.jetbrains.com/issue/KT-30120) False positive "Unused equals expression" in evaluate expression window
- [`KT-30730`](https://youtrack.jetbrains.com/issue/KT-30730) Missing tooltip for "Kotlin variables view" button
- [`KT-30919`](https://youtrack.jetbrains.com/issue/KT-30919) Debugger's "Kotlin View" doesn't show variables inside lambdas
- [`KT-30976`](https://youtrack.jetbrains.com/issue/KT-30976) Debugger: No access to receiver evaluating named parameters during call to extension function
- [`KT-31418`](https://youtrack.jetbrains.com/issue/KT-31418) java.lang.ClassCastException : java.lang.annotation.Annotation[] cannot be cast to byte[]
- [`KT-31510`](https://youtrack.jetbrains.com/issue/KT-31510) isDumb should be used only under read action: KotlinEvaluator
- [`KT-31702`](https://youtrack.jetbrains.com/issue/KT-31702) Debugger can't stop on breakpoint on `Unit` expression from coroutine context
- [`KT-31709`](https://youtrack.jetbrains.com/issue/KT-31709) Evaluate: "IllegalArgumentException: Parameter specified as non-null is null: method org.jetbrains.kotlin.codegen.FrameMapBase.getIndex, parameter descriptor" with nested lambda member access
- [`KT-24829`](https://youtrack.jetbrains.com/issue/KT-24829) Access to coroutineContext in 'Evaluate expression'

### IDE. Gradle

- [`KT-19693`](https://youtrack.jetbrains.com/issue/KT-19693) Import package prefix from Gradle
- [`KT-30667`](https://youtrack.jetbrains.com/issue/KT-30667) Dependencies of a module on a multiplatform one with a JVM target and `withJava()` configured, are incorrectly resolved in IDE
- [`KT-32300`](https://youtrack.jetbrains.com/issue/KT-32300) Add possibility to distinguish kotlin source root from java source root
- [`KT-31014`](https://youtrack.jetbrains.com/issue/KT-31014) Gradle, JS: Webpack watch mode
- [`KT-31843`](https://youtrack.jetbrains.com/issue/KT-31843) Memory leak caused by KOTLIN_TARGET_DATA_NODE on project reimport

### IDE. Gradle. Script

- [`KT-31779`](https://youtrack.jetbrains.com/issue/KT-31779) "Highlighting in scripts is not available"
- [`KT-30638`](https://youtrack.jetbrains.com/issue/KT-30638) "Highlighting in scripts is not available until all Script Dependencies are loaded" in Diff viewer
- [`KT-30974`](https://youtrack.jetbrains.com/issue/KT-30974) Script dependencies resolution failed error while trying to use Kotlin for Gradle
- [`KT-31440`](https://youtrack.jetbrains.com/issue/KT-31440) Add link to Gradle Kotlin DSL logs when script dependencies resolution process fails
- [`KT-32483`](https://youtrack.jetbrains.com/issue/KT-32483) CNFE org.gradle.kotlin.dsl.KotlinBuildScript on creating new Gradle Kotlin project from wizard
- [`KT-21501`](https://youtrack.jetbrains.com/issue/KT-21501) build.gradle.kts displays failures if not using java sdk for module

### IDE. Inspections and Intentions

#### New Features

- [`KT-8958`](https://youtrack.jetbrains.com/issue/KT-8958) ReplaceWith intention message could be more helpful in case of generic substitution
- [`KT-12515`](https://youtrack.jetbrains.com/issue/KT-12515) Quickfix "by Delegates.notNull()" as replacement for "lateinit" for primitive type
- [`KT-14344`](https://youtrack.jetbrains.com/issue/KT-14344) Suggest to replace manual range with explicit `indices` call or iteration over collection
- [`KT-17916`](https://youtrack.jetbrains.com/issue/KT-17916) Import popup does not indicate deprecated classes
- [`KT-23501`](https://youtrack.jetbrains.com/issue/KT-23501) Add intention for converting ordinary properties to 'lazy' and vise versa
- [`KT-25006`](https://youtrack.jetbrains.com/issue/KT-25006) Add inspection "'equals()' between objects of inconvertible primitive / enum / string types"
- [`KT-27353`](https://youtrack.jetbrains.com/issue/KT-27353) Quickfix to add a constructor parameter from parent class to child class
- [`KT-30124`](https://youtrack.jetbrains.com/issue/KT-30124) Add inspection to replace java.util.Arrays.equals with contentEquals
- [`KT-30640`](https://youtrack.jetbrains.com/issue/KT-30640) Add inspection for check/require/checkNotNull/requireNotNull
- [`KT-30775`](https://youtrack.jetbrains.com/issue/KT-30775) Inspection for the case when one lateinit var overrides another lateinit var
- [`KT-31476`](https://youtrack.jetbrains.com/issue/KT-31476) Improve "Create expect..." quickfix
- [`KT-31533`](https://youtrack.jetbrains.com/issue/KT-31533) Make "Add operator modifier" an inspection instead of intention
- [`KT-31795`](https://youtrack.jetbrains.com/issue/KT-31795) Inspection: simplify property setter with custom visibility
- [`KT-31924`](https://youtrack.jetbrains.com/issue/KT-31924) Make "add import" intention more flexible based on caret position
- [`KT-30970`](https://youtrack.jetbrains.com/issue/KT-30970) No warning for empty `if` operator and `also`method

#### Fixes

- [`KT-12567`](https://youtrack.jetbrains.com/issue/KT-12567) "Introduce 'when' subject" intention does not work for "this" in extension function
- [`KT-14369`](https://youtrack.jetbrains.com/issue/KT-14369) "Replace elvis expression with 'if" intention produces boilerplate code for 'return' in RHS
- [`KT-16067`](https://youtrack.jetbrains.com/issue/KT-16067) "Replace 'if' expression with elvis expression" suggests replacing an idiomatic code with non-idiomatic
- [`KT-19643`](https://youtrack.jetbrains.com/issue/KT-19643) Tune or disable the FoldInitializerAndIfToElvis inspection
- [`KT-24439`](https://youtrack.jetbrains.com/issue/KT-24439) No method imports suggested
- [`KT-25786`](https://youtrack.jetbrains.com/issue/KT-25786) False positive "Not-null extension receiver of inline function can be made nullable" with `operator fun invoke`
- [`KT-25905`](https://youtrack.jetbrains.com/issue/KT-25905) False positive for 'LeakingThis' on a method call in enum class body
- [`KT-27074`](https://youtrack.jetbrains.com/issue/KT-27074) False positive "Foldable if-then" with Result type
- [`KT-27550`](https://youtrack.jetbrains.com/issue/KT-27550) "Redundant explicit this" false positive with subclass and extension lambda
- [`KT-27563`](https://youtrack.jetbrains.com/issue/KT-27563) Generate toString in common code shouldn't use java.util.Arrays
- [`KT-27822`](https://youtrack.jetbrains.com/issue/KT-27822) Don't suggest `might be const` on `actual` member declaration
- [`KT-28595`](https://youtrack.jetbrains.com/issue/KT-28595) "Assignment should be lifted out of 'if'" false negative for different but compatible derived types
- [`KT-29192`](https://youtrack.jetbrains.com/issue/KT-29192) "Convert property to function" with explicit generic type loses getter body
- [`KT-29716`](https://youtrack.jetbrains.com/issue/KT-29716) With both explicit and implicit package prefixes "Package name does not match containing directory" inspection suggests not usable quick fix
- [`KT-29731`](https://youtrack.jetbrains.com/issue/KT-29731) Don't suggest `Add val/var to parameter` at expect class constructor
- [`KT-30191`](https://youtrack.jetbrains.com/issue/KT-30191) "Lift out of if" intention isn't suggested for assignment of null
- [`KT-30197`](https://youtrack.jetbrains.com/issue/KT-30197) ReplaceWith for deprecated function adds class literal/callable reference argument above unless it is used in substitution
- [`KT-30627`](https://youtrack.jetbrains.com/issue/KT-30627) "Use property access syntax" produces red code if setter argument is a lambda with implicit SAM conversion
- [`KT-30804`](https://youtrack.jetbrains.com/issue/KT-30804) Property declaration goes to annotation comment when removing only modifier using RemoveModifierFix
- [`KT-30975`](https://youtrack.jetbrains.com/issue/KT-30975) ''when' has only 'else' branch and should be simplified' inspection removes subject variable definition used in else branch
- [`KT-31033`](https://youtrack.jetbrains.com/issue/KT-31033) "Create expect ..." quick fix incorrectly works for a secondary constructor in a multiplatform project
- [`KT-31272`](https://youtrack.jetbrains.com/issue/KT-31272) Expand "create expected ..." quick fix highlighting also to a primary constructor
- [`KT-31278`](https://youtrack.jetbrains.com/issue/KT-31278) Inappropriate "Remove redundant .let call" inspection
- [`KT-31341`](https://youtrack.jetbrains.com/issue/KT-31341) Incorrect quickfix "Replace with Kotlin analog" for conversion to an extension, where the first argument is an expression with an operation
- [`KT-31359`](https://youtrack.jetbrains.com/issue/KT-31359) "Invalid property key" inspection false positive for a bundle with several properties files
- [`KT-31362`](https://youtrack.jetbrains.com/issue/KT-31362) 'Move variable declaration into `when`' quickfix comments left brace with EOL comment
- [`KT-31443`](https://youtrack.jetbrains.com/issue/KT-31443) Remove braces intention places caret in a wrong place
- [`KT-31446`](https://youtrack.jetbrains.com/issue/KT-31446) Incorrect quick fix “Create expected class" for inline class with parameter with actual
- [`KT-31518`](https://youtrack.jetbrains.com/issue/KT-31518) Incorrect "Create expect function" for primary constructor
- [`KT-31673`](https://youtrack.jetbrains.com/issue/KT-31673) Only `when` keyword should be highlighted in WhenWithOnlyElseInspection
- [`KT-31716`](https://youtrack.jetbrains.com/issue/KT-31716) Decrease severity of PackageDirectoryMismatchInspection to INFO
- [`KT-31717`](https://youtrack.jetbrains.com/issue/KT-31717) Decrease severity of RemoveCurlyBracesFromTemplateInspection
- [`KT-31816`](https://youtrack.jetbrains.com/issue/KT-31816) "Package directive doesn't match file location" for root package is invisible in editor
- [`KT-31954`](https://youtrack.jetbrains.com/issue/KT-31954) MoveVariableDeclarationIntoWhen should move the caret to the subject expression
- [`KT-32001`](https://youtrack.jetbrains.com/issue/KT-32001) Wrong quickfixes for TOO_MANY_ARGUMENTS
- [`KT-32010`](https://youtrack.jetbrains.com/issue/KT-32010) Convert ReplaceSingleLineLetIntention to inspections
- [`KT-32046`](https://youtrack.jetbrains.com/issue/KT-32046) False negative "Redundant qualifier name" with class literal
- [`KT-32112`](https://youtrack.jetbrains.com/issue/KT-32112) False positive "Redundant qualifier name"
- [`KT-32318`](https://youtrack.jetbrains.com/issue/KT-32318) "Remove argument name" intention does not remove square braces for annotation vararg argument
- [`KT-32320`](https://youtrack.jetbrains.com/issue/KT-32320) False negative "Redundant qualifier name" with local object
- [`KT-32347`](https://youtrack.jetbrains.com/issue/KT-32347) Duplicative "Remove redundant 'public' modifier" suggestion for getter
- [`KT-32365`](https://youtrack.jetbrains.com/issue/KT-32365) "Convert to sealed class" intention should not be suggested when no "class" keyword
- [`KT-32419`](https://youtrack.jetbrains.com/issue/KT-32419) Spurious 'while' has empty body warning when body has explanatory comment
- [`KT-32506`](https://youtrack.jetbrains.com/issue/KT-32506) False negative "Remove redundant qualifier name" with `java.util.ArrayList<Int>()`
- [`KT-32454`](https://youtrack.jetbrains.com/issue/KT-32454) "Replace Java static method with Kotlin analog": invalid quick fix on 'abs()' function
- [`KT-26242`](https://youtrack.jetbrains.com/issue/KT-26242) "Create test" intention does nothing in common module
- [`KT-27208`](https://youtrack.jetbrains.com/issue/KT-27208) IDEA reports about the need to declare abstract or implement abstract method, but this method is @JvmStatic in an interface companion
- [`KT-27555`](https://youtrack.jetbrains.com/issue/KT-27555) `Create actual ...` quick fix does nothing if the corresponding source set directory isn't created yet
- [`KT-28121`](https://youtrack.jetbrains.com/issue/KT-28121) IDE: Warn on java files under "src/main/kotlin" or "src/test/kotlin" source roots
- [`KT-28295`](https://youtrack.jetbrains.com/issue/KT-28295) Use `languageSettings` for a quick fix to enable experimental features in multiplatform projects
- [`KT-28529`](https://youtrack.jetbrains.com/issue/KT-28529) Don't suggest `commonMain` source set as a target of `create expected ...` quick fix for a member of `*Test` source set
- [`KT-28746`](https://youtrack.jetbrains.com/issue/KT-28746) “Create actual class” quick fix creates invalid file when is called from files located in package directory but don't have package name
- [`KT-30622`](https://youtrack.jetbrains.com/issue/KT-30622) Add names to call arguments starting from given argument
- [`KT-31404`](https://youtrack.jetbrains.com/issue/KT-31404) Redundant 'requireNotNull' or 'checkNotNull' inspection: don't remove first argument
- [`KT-32705`](https://youtrack.jetbrains.com/issue/KT-32705) "Create expect" quick fix adds `actual` modifier to a `const`/`lateinit` declaration without a warning
- [`KT-32967`](https://youtrack.jetbrains.com/issue/KT-32967) Warning about incorrectly placed Java source file isn't automatically dismissed on move of the file to the proper source root

### IDE. JS

- [`KT-31895`](https://youtrack.jetbrains.com/issue/KT-31895) New Project wizard: Kotlin Gradle + Kotlin/JS for Node.js: incorrect DSL is inserted

### IDE. KDoc

- [`KT-30985`](https://youtrack.jetbrains.com/issue/KT-30985) Missing line break in quick doc for enum constant

### IDE. Multiplatform

- [`KT-29757`](https://youtrack.jetbrains.com/issue/KT-29757) IDE fails to import transitive dependency of a JVM module to a multiplatform one

### IDE. Navigation

- [`KT-10215`](https://youtrack.jetbrains.com/issue/KT-10215) Kotlin classes are listed after Java classes in the navigation bar

### IDE. Refactorings

- [`KT-29720`](https://youtrack.jetbrains.com/issue/KT-29720) Refactor / Move does not update package statement with implicit prefix
- [`KT-30762`](https://youtrack.jetbrains.com/issue/KT-30762) Inline method produces invalid code for suspend functions with receiver
- [`KT-30748`](https://youtrack.jetbrains.com/issue/KT-30748) 100+ Seconds UI Freeze on performing a Move Refactoring on a file with a lot of usages (KotlinOptimizeImports in thread dump)

### IDE. Scratch

- [`KT-23604`](https://youtrack.jetbrains.com/issue/KT-23604) Scratch: end of line is wrongly indented with the end of scratch line output 
- [`KT-27963`](https://youtrack.jetbrains.com/issue/KT-27963) Make REPL mode in Scratch files incremental
- [`KT-29534`](https://youtrack.jetbrains.com/issue/KT-29534) Line output jumps to the next line together with cursor
- [`KT-32791`](https://youtrack.jetbrains.com/issue/KT-32791) "Access is allowed from event dispatch thread only" while working with a scratch file

### IDE. Script

- [`KT-25187`](https://youtrack.jetbrains.com/issue/KT-25187) Kotlin script in src: warning: classpath entry points to a non-existent location on JDK 9+
- [`KT-31152`](https://youtrack.jetbrains.com/issue/KT-31152) Errors in IDE when different Java Sdk are set as Project SDK and as Gradle JVM
- [`KT-31521`](https://youtrack.jetbrains.com/issue/KT-31521) CNFE „org.jetbrains.kotlin.idea.caches.project.ScriptBinariesScopeCache“ on creating new Gradle based project
- [`KT-31826`](https://youtrack.jetbrains.com/issue/KT-31826) Gradle clean task causes IDEA to lose kotlin scripting configuration
- [`KT-31837`](https://youtrack.jetbrains.com/issue/KT-31837) TargetPlatform for scripts should depends on scriptDefinition.additionalArguments
- [`KT-30690`](https://youtrack.jetbrains.com/issue/KT-30690) Highlighting for scripts in diff view doesn't work for left part
- [`KT-32061`](https://youtrack.jetbrains.com/issue/KT-32061) Check classpath jars before applying script compilation result from file attributes
- [`KT-32554`](https://youtrack.jetbrains.com/issue/KT-32554) Freezes in ScriptDependenciesUpdater

### IDE. Tests Support

- [`KT-30814`](https://youtrack.jetbrains.com/issue/KT-30814) MPP, 191 platform: with Gradle test runner run configuration for platform test is created without tasks

### IDE. Wizards

- [`KT-32105`](https://youtrack.jetbrains.com/issue/KT-32105) MPP project wizard: add option for Kotlin Gradle DSL

### JS. Tools

- [`KT-31527`](https://youtrack.jetbrains.com/issue/KT-31527) Keep generating empty `jsTest` task
- [`KT-31565`](https://youtrack.jetbrains.com/issue/KT-31565) Gradle/JS: `npmResolve` is never UP-TO-DATE
- [`KT-32326`](https://youtrack.jetbrains.com/issue/KT-32326) Gradle, test runner: support postponing test running error reporting at the end of the build
- [`KT-32393`](https://youtrack.jetbrains.com/issue/KT-32393) Gradle, JS: Resolve projects lazily
- [`KT-31560`](https://youtrack.jetbrains.com/issue/KT-31560) Gradle: provide descriptions for JS tasks
- [`KT-31563`](https://youtrack.jetbrains.com/issue/KT-31563) Gradle/JS: npmResolve fails with "Invalid version" when user project's version does not match npm rules
- [`KT-31566`](https://youtrack.jetbrains.com/issue/KT-31566) Gradle/JS: with explicit call to `nodejs { testTask { useNodeJs() } }` configuration fails : "Could not find which method to invoke"
- [`KT-31694`](https://youtrack.jetbrains.com/issue/KT-31694) Gradle, NPM, windows: creating symlink requires administrator privilege

### Libraries

- [`KT-29372`](https://youtrack.jetbrains.com/issue/KT-29372) measureTime that returns both the result of block and elapsed time
- [`KT-32083`](https://youtrack.jetbrains.com/issue/KT-32083) Incorrect ReplaceWith annotation on kotlin.js.pow
- [`KT-12749`](https://youtrack.jetbrains.com/issue/KT-12749) Provide Int.bitCount, Long.bitCount etc.
- [`KT-32359`](https://youtrack.jetbrains.com/issue/KT-32359) Common Array.fill
- [`KT-33225`](https://youtrack.jetbrains.com/issue/KT-33225) JS: Incorrect conversion of infinite Double to Long

### Reflection

- [`KT-22923`](https://youtrack.jetbrains.com/issue/KT-22923) Reflection getMemberProperties fails: kotlin.reflect.jvm.internal.KotlinReflectionInternalError
- [`KT-31318`](https://youtrack.jetbrains.com/issue/KT-31318) "KotlinReflectionInternalError: Method is not supported" on accessing array class annotation parameter

### Tools. Daemon

- [`KT-31550`](https://youtrack.jetbrains.com/issue/KT-31550) NSME org.jetbrains.kotlin.com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem.clearHandlersCache()V on compileKotlin task with plugin from master
- [`KT-32490`](https://youtrack.jetbrains.com/issue/KT-32490) Compiler daemon tests fail on windows due to directory name being too long
- [`KT-32950`](https://youtrack.jetbrains.com/issue/KT-32950) Daemon should inherit "-XX:MaxMetaspaceSize" of client VM
- [`KT-32992`](https://youtrack.jetbrains.com/issue/KT-32992) Enable assertions in Kotlin Compile Daemon
- [`KT-33027`](https://youtrack.jetbrains.com/issue/KT-33027) Compilation with daemon fails, because IncrementalModuleInfo#serialVersionUID does not match

### Tools. CLI

- [`KT-33177`](https://youtrack.jetbrains.com/issue/KT-33177) Introduce compiler flags -Xinline-classes and -Xpolymorphic-signature as a higher priority than -XXLanguage

### Tools. Compiler Plugins

- [`KT-28824`](https://youtrack.jetbrains.com/issue/KT-28824) Add jvm-abi-gen-embeddable for use with embeddable compiler
- [`KT-31279`](https://youtrack.jetbrains.com/issue/KT-31279) JPS build with compiler plugin and "Keep compiler alive = No" fails with CCE: "Cannot cast NoArgComponentRegistrar to ComponentRegistrar" at ServiceLoaderLite.loadImplementations()
- [`KT-32346`](https://youtrack.jetbrains.com/issue/KT-32346) kotlinx.serialization: Performance problems with completion/intellisense

### Tools. Gradle

#### New Features

- [`KT-26655`](https://youtrack.jetbrains.com/issue/KT-26655) Precise metadata publishing and consumption for new MPP
- [`KT-31018`](https://youtrack.jetbrains.com/issue/KT-31018) Gradle, JS: yarn
- [`KT-31703`](https://youtrack.jetbrains.com/issue/KT-31703) Gradle, JS: automatically download d.ts and generate kotlin/js external declarations using dukat
- [`KT-31890`](https://youtrack.jetbrains.com/issue/KT-31890) Gradle, JS, webpack: provide property with full bundle file path
- [`KT-32015`](https://youtrack.jetbrains.com/issue/KT-32015) Gradle, JS: resolve configuration only while executing tasks of specific projects
- [`KT-32136`](https://youtrack.jetbrains.com/issue/KT-32136) Gradle, test runner: handle case when test runtime exits abnormally
- [`KT-26256`](https://youtrack.jetbrains.com/issue/KT-26256) In new MPP, support Java compilation in JVM targets
- [`KT-30573`](https://youtrack.jetbrains.com/issue/KT-30573) Gradle, JS: enable source maps by default, change paths relative to node_modules directory
- [`KT-30747`](https://youtrack.jetbrains.com/issue/KT-30747) Gradle, JS tests: provide option to disable test configuration per target
- [`KT-31010`](https://youtrack.jetbrains.com/issue/KT-31010) Gradle, JS tests: Mocha
- [`KT-31011`](https://youtrack.jetbrains.com/issue/KT-31011) Gradle, JS tests: Karma
- [`KT-31013`](https://youtrack.jetbrains.com/issue/KT-31013) Gradle, JS: Webpack
- [`KT-31016`](https://youtrack.jetbrains.com/issue/KT-31016) Gradle: yarn downloading
- [`KT-31017`](https://youtrack.jetbrains.com/issue/KT-31017) Gradle, yarn: support workspaces
- [`KT-31697`](https://youtrack.jetbrains.com/issue/KT-31697) Gradle, NPM: report about clashes in packages_imported

#### Performance Improvements

- [`KT-29538`](https://youtrack.jetbrains.com/issue/KT-29538) AndroidSubPlugin#getCommonResDirectories is very slow

#### Fixes

- [`KT-29343`](https://youtrack.jetbrains.com/issue/KT-29343) Kotlin MPP source set dependencies are not properly propagated to tests in Android projects
- [`KT-30691`](https://youtrack.jetbrains.com/issue/KT-30691) Gradle, JS tests: Parent operation with id 947 not available when all tests passed
- [`KT-31917`](https://youtrack.jetbrains.com/issue/KT-31917) Gradle, JS: transitive dependency between compilations in same project doesn't work
- [`KT-31985`](https://youtrack.jetbrains.com/issue/KT-31985) Gradle, JS: webpack not working on windows
- [`KT-32072`](https://youtrack.jetbrains.com/issue/KT-32072) Gradle, JS: browser() in DSL triggers project.evaluate()
- [`KT-32204`](https://youtrack.jetbrains.com/issue/KT-32204) In an MPP, a dependency that is added to a non-root source set is incorrectly analyzed for source sets visibility
- [`KT-32225`](https://youtrack.jetbrains.com/issue/KT-32225) In an MPP, if a dependency is added to a source set that does not take part in published compilations, it is not correctly analyzed in source set visibility inference
- [`KT-32564`](https://youtrack.jetbrains.com/issue/KT-32564) Provide a flag to enable/disable hierarchical multiplatform mechanism in Gradle
- [`KT-31023`](https://youtrack.jetbrains.com/issue/KT-31023) Update Gradle module metadata warning in MPP publishing
- [`KT-31696`](https://youtrack.jetbrains.com/issue/KT-31696) Gradle, NPM: select one version between tools and all of compile configurations
- [`KT-31891`](https://youtrack.jetbrains.com/issue/KT-31891) Gradle: JS or Native tests execution: `build --scan` fails with ISE "Expected attachment of type ... but did not find it"
- [`KT-32210`](https://youtrack.jetbrains.com/issue/KT-32210) Kapt randomly fails with java.io.UTFDataFormatException
- [`KT-32706`](https://youtrack.jetbrains.com/issue/KT-32706) Gradle target "jsBrowserWebpack" should use output of JS compile task as input
- [`KT-32697`](https://youtrack.jetbrains.com/issue/KT-32697) [Tests] org.jetbrains.kotlin.gradle.SubpluginsIT
- [`KT-33246`](https://youtrack.jetbrains.com/issue/KT-33246) Kotlin JS & Native tests + Gradle 5.6: No value has been specified for property 'binaryResultsDirectory'


### Tools. Incremental Compile

- [`KT-31310`](https://youtrack.jetbrains.com/issue/KT-31310) Incremental build of Kotlin/JS project fails with KNPE at IncrementalJsCache.nonDirtyPackageParts()

### Tools. J2K

#### New Features

- [`KT-30776`](https://youtrack.jetbrains.com/issue/KT-30776) New J2K
- [`KT-31836`](https://youtrack.jetbrains.com/issue/KT-31836) Suggest user to configure Kotlin in the project  when running new J2K file conversion
- [`KT-32512`](https://youtrack.jetbrains.com/issue/KT-32512) ReplaceJavaStaticMethodWithKotlinAnalogInspection: add more cases for java.util.Arrays

#### Fixes

- [`KT-15791`](https://youtrack.jetbrains.com/issue/KT-15791) J2K converts class literals including redundant generic <*>
- [`KT-31234`](https://youtrack.jetbrains.com/issue/KT-31234) New J2K: Exception occurs on converting Java class to Kotlin
- [`KT-31250`](https://youtrack.jetbrains.com/issue/KT-31250) J2K: caret position of original file is preserved, adding spaces to resulting file
- [`KT-31251`](https://youtrack.jetbrains.com/issue/KT-31251) J2K: Java class with members is converted to Kotlin class with `final` constructor
- [`KT-31252`](https://youtrack.jetbrains.com/issue/KT-31252) J2K: resulted file is not formatted
- [`KT-31254`](https://youtrack.jetbrains.com/issue/KT-31254) J2K: resulted source uses full qualified references instead of imports
- [`KT-31255`](https://youtrack.jetbrains.com/issue/KT-31255) J2K: redundant modifiers in resulted source
- [`KT-31726`](https://youtrack.jetbrains.com/issue/KT-31726) New J2K converts annotation with array parameter to single value parameter
- [`KT-31809`](https://youtrack.jetbrains.com/issue/KT-31809) "Attempt to modify PSI for non-committed Document!" exception and broken kotlin file after new J2K conversion
- [`KT-31821`](https://youtrack.jetbrains.com/issue/KT-31821) J2K: IDEA Ultimate: local variable: CCE: "PsiLocalVariableImpl cannot be cast to class JvmAnnotatedElement" at JavaToJKTreeBuilder$DeclarationMapper.toJK()
- [`KT-32436`](https://youtrack.jetbrains.com/issue/KT-32436) NewJ2K generic field is not initialized after convertion
- [`KT-19327`](https://youtrack.jetbrains.com/issue/KT-19327) Java to Kotlin converter fails to convert code using Java 8 Stream API
- [`KT-21467`](https://youtrack.jetbrains.com/issue/KT-21467) Convert To Kotlin fails when using chained stream.flatmap methods
- [`KT-24677`](https://youtrack.jetbrains.com/issue/KT-24677) j2k creates nullable type for child function but keeps not null type for parent function
- [`KT-32572`](https://youtrack.jetbrains.com/issue/KT-32572) New J2K: Map with complex type as parameter is wrongly converted
- [`KT-32602`](https://youtrack.jetbrains.com/issue/KT-32602) J2K: no conversion of `String.length()` method call to property access of existing String property
- [`KT-32604`](https://youtrack.jetbrains.com/issue/KT-32604) kotlin.NotImplementedError exception occurs on converting Java call of toString method of data class to Kotlin
- [`KT-32609`](https://youtrack.jetbrains.com/issue/KT-32609) New J2K: Comparable class is wrongly converted to Kotlin if parameter of compareTo marked with @NotNull annotation
- [`KT-32693`](https://youtrack.jetbrains.com/issue/KT-32693) New J2K is throwing „Read access is allowed from event dispatch thread or inside read-action only“ on converting Java code inside Evaluate Expression window
- [`KT-32702`](https://youtrack.jetbrains.com/issue/KT-32702) New J2K: lambda with method reference is converted to lamdba with excessive parameter declaration
- [`KT-32835`](https://youtrack.jetbrains.com/issue/KT-32835) New J2K: NumberFormatException occurs on converting binary literals
- [`KT-32837`](https://youtrack.jetbrains.com/issue/KT-32837) J2K: NumberFormatException occurs on converting literals with underscore characters
- [`KT-22412`](https://youtrack.jetbrains.com/issue/KT-22412) J2K: Intention to replace if(...) throw IAE with require
- [`KT-33371`](https://youtrack.jetbrains.com/issue/KT-33371) Add an ability to switch between old and new J2K via settings window
- [`KT-32863`](https://youtrack.jetbrains.com/issue/KT-32863) New J2K: IllegalArgumentException occurs on Kotlin configuration in java project in Android Studio

### Tools. JPS

- [`KT-27181`](https://youtrack.jetbrains.com/issue/KT-27181) Compiler arguments are listed twice on JPS build of Gradle-based project
- [`KT-13563`](https://youtrack.jetbrains.com/issue/KT-13563) Kotlin jps-plugin should allow to instrument bytecode from Intellij IDEA.

### Tools. REPL

- [`KT-15125`](https://youtrack.jetbrains.com/issue/KT-15125) Support JSR 223 bindings directly via script variables
- [`KT-32085`](https://youtrack.jetbrains.com/issue/KT-32085) Kotlinc REPL: "java.lang.NoClassDefFoundError: org/jline/reader/LineReaderBuilder"

### Tools. Scripts

- [`KT-28137`](https://youtrack.jetbrains.com/issue/KT-28137) Implement result/return value for the regular (non-REPL) scripts

### Tools. kapt

- [`KT-30578`](https://youtrack.jetbrains.com/issue/KT-30578) `build/generated/source/kaptKotlin` is added as source directory to `main` instead of `jvmMain` when jvm { withJava() } is configured in a multiplatform project
- [`KT-30739`](https://youtrack.jetbrains.com/issue/KT-30739) Kapt generated sources are not visible from the IDE when "Create separate module per source set" is disabled
- [`KT-31127`](https://youtrack.jetbrains.com/issue/KT-31127) Kotlin-generating processor which uses Filer API breaks JavaCompile task
- [`KT-31378`](https://youtrack.jetbrains.com/issue/KT-31378) v1.3.31: NoSuchElementException in kapt when kapt.incremental.apt=true
- [`KT-32535`](https://youtrack.jetbrains.com/issue/KT-32535) Kapt aptMode=compile don't include files generated at `kapt.kotlin.generated` as sources to compile
- [`KT-31471`](https://youtrack.jetbrains.com/issue/KT-31471) KAPT prints "IncrementalProcessor" instead of processor name in verbose mode

## 1.3.41

### Compiler

- [`KT-31981`](https://youtrack.jetbrains.com/issue/KT-31981) New type inference asks to use ?. on non-null local variable
- [`KT-32029`](https://youtrack.jetbrains.com/issue/KT-32029) Exception when callable reference is resolved against unresolved type
- [`KT-32037`](https://youtrack.jetbrains.com/issue/KT-32037) No coercion to Unit for last expression with lambda in code block
- [`KT-32038`](https://youtrack.jetbrains.com/issue/KT-32038) Unsubstituted stub type cause type mismatch later for builder inference
- [`KT-32051`](https://youtrack.jetbrains.com/issue/KT-32051) NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER on matching Nothing with generic type parameter
- [`KT-32081`](https://youtrack.jetbrains.com/issue/KT-32081) New type inference fails involving Either and Nothing
- [`KT-32089`](https://youtrack.jetbrains.com/issue/KT-32089) False positive IMPLICIT_NOTHING_AS_TYPE_PARAMETER with lambdas
- [`KT-32094`](https://youtrack.jetbrains.com/issue/KT-32094) NI: member from star import has higher resolution priority than member imported by FQN
- [`KT-32116`](https://youtrack.jetbrains.com/issue/KT-32116) Type inference for HashMap<*,*> fails but compiles
- [`KT-32123`](https://youtrack.jetbrains.com/issue/KT-32123) Wrong unused import for extension method
- [`KT-32133`](https://youtrack.jetbrains.com/issue/KT-32133) Regression in Kotlin 1.3.40 new inference engine
- [`KT-32134`](https://youtrack.jetbrains.com/issue/KT-32134) `java.lang.Throwable: Resolution error of this type shouldn't occur for resolve try as a call` for incomplete try-construction
- [`KT-32143`](https://youtrack.jetbrains.com/issue/KT-32143) 1.3.40 new inference: backward incompatibility in method calls with multiple SAM arguments
- [`KT-32154`](https://youtrack.jetbrains.com/issue/KT-32154) setOf(Map.Entry<*, *>::key) gives error on IDE
- [`KT-32157`](https://youtrack.jetbrains.com/issue/KT-32157) Issue with new type inference in unbounded generics
- [`KT-32175`](https://youtrack.jetbrains.com/issue/KT-32175) New Type Inference Algorithm, RxJava and IDE-Compiler Inconsistency
- [`KT-32184`](https://youtrack.jetbrains.com/issue/KT-32184) NI: Argument for @NotNull parameter 'type' of org/jetbrains/kotlin/types/CommonSupertypes.depth must not be null
- [`KT-32187`](https://youtrack.jetbrains.com/issue/KT-32187) Exception when using callable reference with an unresolved LHS
- [`KT-32218`](https://youtrack.jetbrains.com/issue/KT-32218) Cannot call get on a Map<out Any,Any> with new type system
- [`KT-32230`](https://youtrack.jetbrains.com/issue/KT-32230) New inference not working with RxJava combineLatest
- [`KT-32235`](https://youtrack.jetbrains.com/issue/KT-32235) New type inference failure with `in` check

### JavaScript

- [`KT-32215`](https://youtrack.jetbrains.com/issue/KT-32215) Reified generic doesn't work with `ByteArray` on js

### Tools. CLI

- [`KT-32272`](https://youtrack.jetbrains.com/issue/KT-32272) kotlinc - no main manifest attribute, in hello.jar

### Tools. REPL

- [`KT-32085`](https://youtrack.jetbrains.com/issue/KT-32085) Kotlinc REPL: "java.lang.NoClassDefFoundError: org/jline/reader/LineReaderBuilder"

### Tools. Scripts

- [`KT-32169`](https://youtrack.jetbrains.com/issue/KT-32169) Kotlin 1.3.40 - Crash on running *.main.kts script: "NoSuchMethodError: kotlin.script.templates.standard.ScriptTemplateWithArgs.<init>"
- [`KT-32206`](https://youtrack.jetbrains.com/issue/KT-32206) Custom script definitions not loaded in the cli compiler

## 1.3.40

### Android

#### Fixes

- [`KT-12402`](https://youtrack.jetbrains.com/issue/KT-12402) Android DataBinding work correctly but the IDE show it as error
- [`KT-31432`](https://youtrack.jetbrains.com/issue/KT-31432) Remove obsolete code introduced in KT-12402

### Compiler

#### New Features

- [`KT-29915`](https://youtrack.jetbrains.com/issue/KT-29915) Implement `typeOf` on JVM
- [`KT-30467`](https://youtrack.jetbrains.com/issue/KT-30467) Provide a way to to save compiled script(s) as a jar

#### Performance Improvements

- [`KT-17755`](https://youtrack.jetbrains.com/issue/KT-17755) Optimize trimIndent and trimMargin on constant strings
- [`KT-30603`](https://youtrack.jetbrains.com/issue/KT-30603) Compiler performance issue: VariableLivenessKt.useVar performance

#### Fixes

- [`KT-19227`](https://youtrack.jetbrains.com/issue/KT-19227) Load built-ins from dependencies by default in the compiler, support erroneous "fallback" built-ins
- [`KT-23426`](https://youtrack.jetbrains.com/issue/KT-23426) Actual typealias to Java enum does not match expected enum because of modality
- [`KT-23854`](https://youtrack.jetbrains.com/issue/KT-23854) Inference for common type of two captured types
- [`KT-25105`](https://youtrack.jetbrains.com/issue/KT-25105) False-positive warning "Remove final upper bound" on generic override
- [`KT-25302`](https://youtrack.jetbrains.com/issue/KT-25302) New inference: "Type mismatch" between star projection and `Any?` type argument in specific case
- [`KT-25433`](https://youtrack.jetbrains.com/issue/KT-25433) Wrong order of fixing type variables for callable references
- [`KT-26386`](https://youtrack.jetbrains.com/issue/KT-26386) Front-end recursion problem while analyzing contract function with call expression of self in implies
- [`KT-26412`](https://youtrack.jetbrains.com/issue/KT-26412) Wrong LVT generated if decomposed parameter of suspend lambda is not the first parameter.
- [`KT-27097`](https://youtrack.jetbrains.com/issue/KT-27097) JvmMultifileClass + JvmName causes NoSuchMethodError on sealed class hierarchy for top-level members
- [`KT-28534`](https://youtrack.jetbrains.com/issue/KT-28534) Local variable entries are missing in LVT for suspend lambda parameters
- [`KT-28535`](https://youtrack.jetbrains.com/issue/KT-28535) Rename `result` to `$result` in coroutines' LVT
- [`KT-29184`](https://youtrack.jetbrains.com/issue/KT-29184) Implement inference for coroutines according to the @BuilderInference contract in NI
- [`KT-29772`](https://youtrack.jetbrains.com/issue/KT-29772) Contracts don't work if `contract` function is fully qualified (FQN)
- [`KT-29790`](https://youtrack.jetbrains.com/issue/KT-29790) Incorrect version requirement in metadata of anonymous class for suspend lambda
- [`KT-29948`](https://youtrack.jetbrains.com/issue/KT-29948) NI: incorrect DSLMarker behaviour with generic star projection
- [`KT-30021`](https://youtrack.jetbrains.com/issue/KT-30021) +NewInference on Kotlin Native :: java.lang.StackOverflowError
- [`KT-30242`](https://youtrack.jetbrains.com/issue/KT-30242) Statements are not coerced to Unit in last expressions of lambda
- [`KT-30243`](https://youtrack.jetbrains.com/issue/KT-30243) Include FIR modules into compiler
- [`KT-30250`](https://youtrack.jetbrains.com/issue/KT-30250) Rewrite at slice exception for callable reference argument inside delegated expression
- [`KT-30292`](https://youtrack.jetbrains.com/issue/KT-30292) Reference to function is unresolved when LHS is a star-projected type
- [`KT-30293`](https://youtrack.jetbrains.com/issue/KT-30293) Wrong intersection type for common supertype from String and integer type
- [`KT-30370`](https://youtrack.jetbrains.com/issue/KT-30370) Call is completed too early when there is "Nothing" constraint
- [`KT-30405`](https://youtrack.jetbrains.com/issue/KT-30405) Support expected type from cast in new inference
- [`KT-30406`](https://youtrack.jetbrains.com/issue/KT-30406) Fix testIfOrWhenSpecialCall test for new inference
- [`KT-30590`](https://youtrack.jetbrains.com/issue/KT-30590) Report diagnostic about not enough information for inference in NI
- [`KT-30620`](https://youtrack.jetbrains.com/issue/KT-30620) Exception from the compiler when coroutine-inference is involved even with the explicitly specified types
- [`KT-30656`](https://youtrack.jetbrains.com/issue/KT-30656) Exception is occurred when functions with implicit return-stub types are involved in builder-inference
- [`KT-30658`](https://youtrack.jetbrains.com/issue/KT-30658) Exception from the compiler when getting callable reference to a suspend function
- [`KT-30661`](https://youtrack.jetbrains.com/issue/KT-30661) Disable SAM conversions to Kotlin functions in new-inference by default
- [`KT-30676`](https://youtrack.jetbrains.com/issue/KT-30676) Overload resolution ambiguity when there is a callable reference argument and candidates with different functional return types
- [`KT-30694`](https://youtrack.jetbrains.com/issue/KT-30694) No debug metadata is generated for suspend lambdas which capture crossinline
- [`KT-30724`](https://youtrack.jetbrains.com/issue/KT-30724) False positive error about missing equals when one of the operands is incorrectly inferred to Nothing
- [`KT-30734`](https://youtrack.jetbrains.com/issue/KT-30734) No smartcast inside lambda literal in then/else "if" branch
- [`KT-30737`](https://youtrack.jetbrains.com/issue/KT-30737) Try analysing callable reference preemptively
- [`KT-30780`](https://youtrack.jetbrains.com/issue/KT-30780) Compiler crashes on 'private inline' function accessing private constant in 'inline class' (regression)
- [`KT-30808`](https://youtrack.jetbrains.com/issue/KT-30808) NI: False negative SPREAD_OF_NULLABLE with USELESS_ELVIS_RIGHT_IS_NULL
- [`KT-30816`](https://youtrack.jetbrains.com/issue/KT-30816) BasicJvmScriptEvaluator passes constructor parameters in incorrect order
- [`KT-30826`](https://youtrack.jetbrains.com/issue/KT-30826) There isn't report about unsafe call in the new inference (by invalidating smartcast), NPE
- [`KT-30843`](https://youtrack.jetbrains.com/issue/KT-30843) Duplicate JVM class name for expect/actual classes in JvmMultifileClass-annotated file
- [`KT-30853`](https://youtrack.jetbrains.com/issue/KT-30853) Compiler crashes with NewInference and Kotlinx.Coroutines Flow
- [`KT-30927`](https://youtrack.jetbrains.com/issue/KT-30927) Data flow info isn't used for 'this' which is returned from lambda using labeled return
- [`KT-31081`](https://youtrack.jetbrains.com/issue/KT-31081) Implement ArgumentMatch abstraction in new inference
- [`KT-31113`](https://youtrack.jetbrains.com/issue/KT-31113) Fix failing tests from SlicerTestGenerated
- [`KT-31199`](https://youtrack.jetbrains.com/issue/KT-31199) Unresolved callable references with typealias
- [`KT-31339`](https://youtrack.jetbrains.com/issue/KT-31339) Inliner does not remove redundant continuation classes, leading to CNFE in JMH bytecode processing
- [`KT-31346`](https://youtrack.jetbrains.com/issue/KT-31346) Fix diagnostic DSL_SCOPE_VIOLATION for new inference
- [`KT-31356`](https://youtrack.jetbrains.com/issue/KT-31356) False-positive error about violating dsl scope for new-inference
- [`KT-31360`](https://youtrack.jetbrains.com/issue/KT-31360) NI: inconsistently prohibits member usage without explicit receiver specification with star projection and DSL marker
- [`KT-18563`](https://youtrack.jetbrains.com/issue/KT-18563) Do not generate inline reified functions as private in bytecode
- [`KT-20849`](https://youtrack.jetbrains.com/issue/KT-20849) Inference results in Nothing type argument in case of passing 'out T' to 'in T1'
- [`KT-25290`](https://youtrack.jetbrains.com/issue/KT-25290) New inference: KNPE at ResolutionPartsKt.getExpectedTypeWithSAMConversion() on out projection of Java class
- [`KT-26418`](https://youtrack.jetbrains.com/issue/KT-26418) Back-end (JVM) Internal error when compiling decorated suspend inline functions
- [`KT-26925`](https://youtrack.jetbrains.com/issue/KT-26925) Decorated suspend inline function continuation resumes in wrong spot
- [`KT-28999`](https://youtrack.jetbrains.com/issue/KT-28999) Prohibit type parameters for anonymous objects
- [`KT-29307`](https://youtrack.jetbrains.com/issue/KT-29307) New inference: false negative CONSTANT_EXPECTED_TYPE_MISMATCH with a Map
- [`KT-29475`](https://youtrack.jetbrains.com/issue/KT-29475) IllegalArgumentException at getAbstractTypeFromDescriptor with deeply nested expression inside function named with a right parenthesis
- [`KT-29996`](https://youtrack.jetbrains.com/issue/KT-29996) Properly report  errors on attempt to inline bytecode from class files compiled to 1.8 to one compiling to 1.6
- [`KT-30289`](https://youtrack.jetbrains.com/issue/KT-30289) Don't generate annotations on synthetic methods for methods with default values for parameters
- [`KT-30410`](https://youtrack.jetbrains.com/issue/KT-30410) [NI] Front-end recursion problem while analyzing contract function with call expression of self in implies
- [`KT-30411`](https://youtrack.jetbrains.com/issue/KT-30411) Fold recursive types to star-projected ones when inferring type variables
- [`KT-30706`](https://youtrack.jetbrains.com/issue/KT-30706) Passing noinline lambda as (cross)inline parameter result in wrong state-machine
- [`KT-30707`](https://youtrack.jetbrains.com/issue/KT-30707) Java interop of coroutines inside inline functions is broken
- [`KT-30983`](https://youtrack.jetbrains.com/issue/KT-30983) ClassCastException: DeserializedTypeAliasDescriptor cannot be cast to PackageViewDescriptor on star-import of expect enum class actualized with typealias
- [`KT-31242`](https://youtrack.jetbrains.com/issue/KT-31242) "Can't find enclosing method" proguard compilation exception with inline and crossinline
- [`KT-31347`](https://youtrack.jetbrains.com/issue/KT-31347) "IndexOutOfBoundsException: Insufficient maximum stack size" with crossinline and suspend
- [`KT-31354`](https://youtrack.jetbrains.com/issue/KT-31354) Suspend inline functions with crossinline parameters are inaccessible from java
- [`KT-31367`](https://youtrack.jetbrains.com/issue/KT-31367) IllegalStateException: Concrete fake override public open fun (...)  defined in TheIssue[PropertyGetterDescriptorImpl@1a03c376] should have exactly one concrete super-declaration: []
- [`KT-31461`](https://youtrack.jetbrains.com/issue/KT-31461) NI: NONE_APPLICABLE instead of TYPE_MISMATCH when invoking convention plus operator
- [`KT-31503`](https://youtrack.jetbrains.com/issue/KT-31503) Type mismatch with recursive types and SAM conversions
- [`KT-31507`](https://youtrack.jetbrains.com/issue/KT-31507) Enable new type inference algorithm for IDE analysis
- [`KT-31514`](https://youtrack.jetbrains.com/issue/KT-31514) New inference generates multiple errors on generic inline expression with elvis operator
- [`KT-31520`](https://youtrack.jetbrains.com/issue/KT-31520) False positive "not enough information" for constraint with star projection and covariant type
- [`KT-31606`](https://youtrack.jetbrains.com/issue/KT-31606) Rewrite at slice on using callable reference with array access operator
- [`KT-31620`](https://youtrack.jetbrains.com/issue/KT-31620) False-positive "not enough information" for coroutine-inference when target method is assigned to a variable
- [`KT-31624`](https://youtrack.jetbrains.com/issue/KT-31624) Type from declared upper bound in Java is considered more specific than Nothing producing type mismatch later
- [`KT-31860`](https://youtrack.jetbrains.com/issue/KT-31860) Explicit type argument isn't considered as input type causing errors about "only input types"
- [`KT-31866`](https://youtrack.jetbrains.com/issue/KT-31866) Problems with using star-projections on LHS of callable reference
- [`KT-31868`](https://youtrack.jetbrains.com/issue/KT-31868) No type mismatch error when using NoInfer annotation
- [`KT-31941`](https://youtrack.jetbrains.com/issue/KT-31941) Good code red in IDE with smart cast on parameter of a generic type after null check

### IDE

#### New Features

- [`KT-11242`](https://youtrack.jetbrains.com/issue/KT-11242) Action to copy project diagnostic information to clipboard
- [`KT-24292`](https://youtrack.jetbrains.com/issue/KT-24292) Support external nullability annotations
- [`KT-30453`](https://youtrack.jetbrains.com/issue/KT-30453) Add plugin option (registry?) to enable new inference only in IDE

#### Performance Improvements

- [`KT-13841`](https://youtrack.jetbrains.com/issue/KT-13841) Classes and functions should be lazy-parseable
- [`KT-27106`](https://youtrack.jetbrains.com/issue/KT-27106) Performance issue with optimize imports
- [`KT-30442`](https://youtrack.jetbrains.com/issue/KT-30442) Several second lag on project open in KotlinNonJvmSourceRootConverterProvider
- [`KT-30644`](https://youtrack.jetbrains.com/issue/KT-30644) ConfigureKotlinInProjectUtilsKt freezes UI

#### Fixes

- [`KT-7380`](https://youtrack.jetbrains.com/issue/KT-7380) Imports insertion on paste does not work correctly when there were alias imports in the source file
- [`KT-10512`](https://youtrack.jetbrains.com/issue/KT-10512) Do not delete imports with unresolved parts when optimizing
- [`KT-13048`](https://youtrack.jetbrains.com/issue/KT-13048) "Strip trailing spaces on Save" should not strip trailing spaces inside multiline strings in Kotlin
- [`KT-17375`](https://youtrack.jetbrains.com/issue/KT-17375) Optimize Imports does not remove unused import alias
- [`KT-27385`](https://youtrack.jetbrains.com/issue/KT-27385) Uast: property references should resolve to getters/setters
- [`KT-28627`](https://youtrack.jetbrains.com/issue/KT-28627) Invalid detection of Kotlin jvmTarget inside Idea/gradle build
- [`KT-29267`](https://youtrack.jetbrains.com/issue/KT-29267) Enable ultra-light classes by default
- [`KT-29892`](https://youtrack.jetbrains.com/issue/KT-29892) A lot of threads are waiting in KotlinConfigurationCheckerComponent
- [`KT-30356`](https://youtrack.jetbrains.com/issue/KT-30356) Kotlin facet: all JVM 9+ target platforms are shown as "Target Platform = JVM 9" in Project Structure dialog
- [`KT-30514`](https://youtrack.jetbrains.com/issue/KT-30514) Auto-import with "Add unambiguous imports on the fly" imports enum members from another package
- [`KT-30583`](https://youtrack.jetbrains.com/issue/KT-30583) Kotlin light elements should be `isEquivalentTo` to it's origins
- [`KT-30688`](https://youtrack.jetbrains.com/issue/KT-30688) Memory leak in the PerModulePackageCacheService.onTooComplexChange method
- [`KT-30949`](https://youtrack.jetbrains.com/issue/KT-30949) Optimize Imports removes used import alias
- [`KT-30957`](https://youtrack.jetbrains.com/issue/KT-30957) Kotlin UAST: USimpleNameReferenceExpression in "imports" for class' member resolves incorrectly to class, not to the member
- [`KT-31090`](https://youtrack.jetbrains.com/issue/KT-31090) java.lang.NoSuchMethodError: org.jetbrains.kotlin.idea.UtilsKt.addModuleDependencyIfNeeded on import of a multiplatform project with Android target (191 IDEA + master)
- [`KT-31092`](https://youtrack.jetbrains.com/issue/KT-31092) Don't check all selected files in CheckComponentsUsageSearchAction.update()
- [`KT-31319`](https://youtrack.jetbrains.com/issue/KT-31319) False positive "Unused import" for `provideDelegate` extension
- [`KT-31332`](https://youtrack.jetbrains.com/issue/KT-31332) Kotlin AnnotatedElementsSearch does't support Kotlin `object`
- [`KT-31129`](https://youtrack.jetbrains.com/issue/KT-31129) Call only Kotlin-specific reference contributors for getting Kotlin references from PSI
- [`KT-31693`](https://youtrack.jetbrains.com/issue/KT-31693) Project with no Kotlin: JPS rebuild fails with NCDFE for GradleSettingsService at KotlinMPPGradleProjectTaskRunner.canRun()
- [`KT-31466`](https://youtrack.jetbrains.com/issue/KT-31466) SOE in Java highlighting when a Kotlin ultra-light method is invoked
- [`KT-31723`](https://youtrack.jetbrains.com/issue/KT-31723) Exception from UAST for attempt to infer types inside unresolved call
- [`KT-31842`](https://youtrack.jetbrains.com/issue/KT-31842) UOE: no descriptor for type constructor of TypeVariable(T)
- [`KT-31992`](https://youtrack.jetbrains.com/issue/KT-31992) Fix ColorsIcon.scale(float) compatibility issue between IU-192.5118.30 and 1.3.40-eap-105

### IDE. Completion

- [`KT-29038`](https://youtrack.jetbrains.com/issue/KT-29038) Autocomplete "suspend" into "suspend fun" at top level and class level (except in kts top level)
- [`KT-29398`](https://youtrack.jetbrains.com/issue/KT-29398) Add "arg" postfix template
- [`KT-30511`](https://youtrack.jetbrains.com/issue/KT-30511) Replace extra space after autocompleting data class with file name by parentheses

### IDE. Debugger

- [`KT-10636`](https://youtrack.jetbrains.com/issue/KT-10636) Debugger: can't evaluate call of function type parameter inside inline function
- [`KT-18247`](https://youtrack.jetbrains.com/issue/KT-18247) Debugger: class level watches fail to evaluate outside of class instance context
- [`KT-18263`](https://youtrack.jetbrains.com/issue/KT-18263) Settings / Debugger / Java Type Renderers: unqualified Kotlin class members in Java expressions are shown as errors
- [`KT-23586`](https://youtrack.jetbrains.com/issue/KT-23586) Non-trivial properties autocompletion in evaluation window
- [`KT-30216`](https://youtrack.jetbrains.com/issue/KT-30216) Evaluate expression: declarations annotated with Experimental (LEVEL.ERROR) fail due to compilation error
- [`KT-30610`](https://youtrack.jetbrains.com/issue/KT-30610) Debugger: Variables view shows second `this` instance for inline function even from the same class as caller function
- [`KT-30714`](https://youtrack.jetbrains.com/issue/KT-30714) Breakpoints are shown as invalid for classes that are not loaded yet
- [`KT-30934`](https://youtrack.jetbrains.com/issue/KT-30934) "InvocationException: Exception occurred in target VM" on debugger breakpoint hit (with kotlintest)
- [`KT-31266`](https://youtrack.jetbrains.com/issue/KT-31266) Kotlin debugger incompatibility with latest 192 nightly: KotlinClassWithDelegatedPropertyRenderer
- [`KT-31785`](https://youtrack.jetbrains.com/issue/KT-31785) Exception on attempt to evaluate local function

### IDE. Gradle

- [`KT-29854`](https://youtrack.jetbrains.com/issue/KT-29854) File collection dependency does not work with NMPP+JPS
- [`KT-30531`](https://youtrack.jetbrains.com/issue/KT-30531) Gradle: NodeJS downloading
- [`KT-30767`](https://youtrack.jetbrains.com/issue/KT-30767) Kotlin import uses too much memory when working with big projects
- [`KT-29564`](https://youtrack.jetbrains.com/issue/KT-29564) kotlin.parallel.tasks.in.project=true causes idea to create kotlin modules with target JVM 1.6
- [`KT-31014`](https://youtrack.jetbrains.com/issue/KT-31014) Gradle, JS: Webpack watch mode
- [`KT-31843`](https://youtrack.jetbrains.com/issue/KT-31843) Memory leak caused by KOTLIN_TARGET_DATA_NODE on project reimport
- [`KT-31952`](https://youtrack.jetbrains.com/issue/KT-31952) Fix compatibility issues with IDEA after fixing IDEA-187832

### IDE. Gradle. Script

- [`KT-30638`](https://youtrack.jetbrains.com/issue/KT-30638) "Highlighting in scripts is not available until all Script Dependencies are loaded" in Diff viewer
- [`KT-31124`](https://youtrack.jetbrains.com/issue/KT-31124) “compileKotlin - configuration not found: kotlinScriptDef, the plugin is probably applied by a mistake” after creating new project with IJ and Kotlin from master
- [`KT-30974`](https://youtrack.jetbrains.com/issue/KT-30974) Script dependencies resolution failed error while trying to use Kotlin for Gradle

### IDE. Hints

- [`KT-30057`](https://youtrack.jetbrains.com/issue/KT-30057) "View->Type info" shows "Type is unknown" for named argument syntax

### IDE. Inspections and Intentions

#### New Features

- [`KT-11629`](https://youtrack.jetbrains.com/issue/KT-11629) Inspection: creating Throwable without throwing it
- [`KT-12392`](https://youtrack.jetbrains.com/issue/KT-12392) Unused import with alias should be highlighted and removed with Optimize Imports
- [`KT-12721`](https://youtrack.jetbrains.com/issue/KT-12721) inspection should be made for converting Integer.toString(int) to int.toString()
- [`KT-13962`](https://youtrack.jetbrains.com/issue/KT-13962) Intention to replace Java collection constructor calls with function calls from stdlib (ArrayList() → arrayListOf())
- [`KT-15537`](https://youtrack.jetbrains.com/issue/KT-15537) Add inspection + intention to replace IntRange.start/endInclusive with first/last
- [`KT-21195`](https://youtrack.jetbrains.com/issue/KT-21195) ReplaceWith intention could save generic type arguments
- [`KT-25262`](https://youtrack.jetbrains.com/issue/KT-25262) Intention: Rename class to containing file name
- [`KT-25439`](https://youtrack.jetbrains.com/issue/KT-25439) Inspection "Map replaceable with EnumMap"
- [`KT-26269`](https://youtrack.jetbrains.com/issue/KT-26269) Inspection to replace associate with associateWith or associateBy
- [`KT-26629`](https://youtrack.jetbrains.com/issue/KT-26629) Inspection to replace `==` operator on Double.NaN with `equals` call
- [`KT-27411`](https://youtrack.jetbrains.com/issue/KT-27411) Inspection and Quickfix to replace System.exit() with exitProcess()
- [`KT-29344`](https://youtrack.jetbrains.com/issue/KT-29344) Convert property initializer to getter: suggest on property name
- [`KT-29666`](https://youtrack.jetbrains.com/issue/KT-29666) Quickfix for "DEPRECATED_JAVA_ANNOTATION": migrate arguments
- [`KT-29798`](https://youtrack.jetbrains.com/issue/KT-29798) Add 'Covariant equals' inspection
- [`KT-29799`](https://youtrack.jetbrains.com/issue/KT-29799) Inspection: class with non-null self-reference as a parameter in its primary constructor
- [`KT-30078`](https://youtrack.jetbrains.com/issue/KT-30078) Add "Add getter/setter" quick fix for uninitialized property
- [`KT-30381`](https://youtrack.jetbrains.com/issue/KT-30381) Inspection + quickfix to replace non-null assertion with return
- [`KT-30389`](https://youtrack.jetbrains.com/issue/KT-30389) Fix to convert argument to Int: suggest roundToInt()
- [`KT-30501`](https://youtrack.jetbrains.com/issue/KT-30501) Add inspection to replace filter { it is Foo } with filterIsInstance<Foo> and filter { it != null } with filterNotNull
- [`KT-30612`](https://youtrack.jetbrains.com/issue/KT-30612) Unused symbol inspection should detect enum entry
- [`KT-30663`](https://youtrack.jetbrains.com/issue/KT-30663) Fully qualified name is added on quick fix for original class name if import alias exists
- [`KT-30725`](https://youtrack.jetbrains.com/issue/KT-30725) Inspection which replaces `.sorted().first()` with `.min()`

#### Fixes

- [`KT-5412`](https://youtrack.jetbrains.com/issue/KT-5412) "Replace non-null assertion with `if` expression" should replace parent expression
- [`KT-13549`](https://youtrack.jetbrains.com/issue/KT-13549) "Package directive doesn't match file location" for root package
- [`KT-14040`](https://youtrack.jetbrains.com/issue/KT-14040) Secondary enum class constructor is marked as "unused" by IDE
- [`KT-18459`](https://youtrack.jetbrains.com/issue/KT-18459) Spring: "Autowiring for Bean Class (Kotlin)" inspection adds not working `@Named` annotation to property
- [`KT-21526`](https://youtrack.jetbrains.com/issue/KT-21526) used class is marked as "never used"
- [`KT-22896`](https://youtrack.jetbrains.com/issue/KT-22896) "Change function signature" quickfix on "x overrides nothing" doesn't rename type arguments
- [`KT-27089`](https://youtrack.jetbrains.com/issue/KT-27089) ReplaceWith quickfix doesn't take into account generic parameter
- [`KT-27821`](https://youtrack.jetbrains.com/issue/KT-27821) SimplifiableCallChain inspection quick fix removes comments for intermediate operations
- [`KT-28485`](https://youtrack.jetbrains.com/issue/KT-28485) Incorrect parameter name after running "Add parameter to function" intention when argument variable is upper case const
- [`KT-28619`](https://youtrack.jetbrains.com/issue/KT-28619) "Add braces to 'if' statement" moves end-of-line comment inside an `if` branch if statement inside `if` is block
- [`KT-29556`](https://youtrack.jetbrains.com/issue/KT-29556) "Remove redundant 'let' call" doesn't rename parameter with convention `invoke` call
- [`KT-29677`](https://youtrack.jetbrains.com/issue/KT-29677) "Specify type explicitly" intention produces invalid output for type escaped with backticks
- [`KT-29764`](https://youtrack.jetbrains.com/issue/KT-29764) "Convert property to function" intention doesn't warn about the property overloads at child class constructor
- [`KT-29812`](https://youtrack.jetbrains.com/issue/KT-29812) False positive for HasPlatformType with member extension on 'dynamic'
- [`KT-29869`](https://youtrack.jetbrains.com/issue/KT-29869) 'WhenWithOnlyElse': possibly useless inspection with false grey warning highlighting during editing the code
- [`KT-30038`](https://youtrack.jetbrains.com/issue/KT-30038) 'Remove redundant Unit" false positive when return type is nullable Unit
- [`KT-30082`](https://youtrack.jetbrains.com/issue/KT-30082) False positive "redundant `.let` call" for lambda functions stored in nullable references
- [`KT-30173`](https://youtrack.jetbrains.com/issue/KT-30173) "Nested lambda has shadowed implicit parameter" is suggested when both parameters are logically the same
- [`KT-30208`](https://youtrack.jetbrains.com/issue/KT-30208) Convert to anonymous object: lambda generic type argument is lost
- [`KT-30215`](https://youtrack.jetbrains.com/issue/KT-30215) No "surround with null" check is suggested for an assignment
- [`KT-30228`](https://youtrack.jetbrains.com/issue/KT-30228) 'Convert to also/apply/run/with' intention behaves differently depending on the position of infix function call
- [`KT-30457`](https://youtrack.jetbrains.com/issue/KT-30457) MoveVariableDeclarationIntoWhen: do not report gray warning on variable declarations taking multiple lines / containing preemptive returns
- [`KT-30481`](https://youtrack.jetbrains.com/issue/KT-30481) Do not report ImplicitNullableNothingType on a function/property that overrides a function/property of type 'Nothing?'
- [`KT-30527`](https://youtrack.jetbrains.com/issue/KT-30527) False positive "Type alias is never used" with import of enum member
- [`KT-30559`](https://youtrack.jetbrains.com/issue/KT-30559) Redundant Getter, Redundant Setter: reduce range to getter/setter header
- [`KT-30565`](https://youtrack.jetbrains.com/issue/KT-30565) False positive "Suspicious 'var' property" inspection with annotated default property getter
- [`KT-30579`](https://youtrack.jetbrains.com/issue/KT-30579) Kotlin-gradle groovy inspections should depend on Groovy plugin
- [`KT-30613`](https://youtrack.jetbrains.com/issue/KT-30613) "Convert to anonymous function" should not insert named argument when interoping with Java functions
- [`KT-30614`](https://youtrack.jetbrains.com/issue/KT-30614) String templates suggest removing curly braces for backtick escaped identifiers
- [`KT-30622`](https://youtrack.jetbrains.com/issue/KT-30622) Add names to call arguments starting from given argument
- [`KT-30637`](https://youtrack.jetbrains.com/issue/KT-30637) False positive "unused constructor" for local class
- [`KT-30669`](https://youtrack.jetbrains.com/issue/KT-30669) Import quick fix does not work for property/function with original name if import alias for them exist
- [`KT-30761`](https://youtrack.jetbrains.com/issue/KT-30761) Replace assert boolean with assert equality produces uncompilable code when compared arguments type are different
- [`KT-30769`](https://youtrack.jetbrains.com/issue/KT-30769) Override quickfix creates "sealed fun"
- [`KT-30833`](https://youtrack.jetbrains.com/issue/KT-30833) Exception after "Introduce Import Alias" if invoke in import
- [`KT-30876`](https://youtrack.jetbrains.com/issue/KT-30876) SimplifyNotNullAssert inspection changes semantics
- [`KT-30900`](https://youtrack.jetbrains.com/issue/KT-30900) Invert 'if' condition respects neither code formatting nor inline comments
- [`KT-30910`](https://youtrack.jetbrains.com/issue/KT-30910) "Use property access syntax" is not suitable as text for inspection problem text
- [`KT-30916`](https://youtrack.jetbrains.com/issue/KT-30916) Quickfix "Remove redundant qualifier name" can't work with user type with generic parameter
- [`KT-31103`](https://youtrack.jetbrains.com/issue/KT-31103) Don't invoke Gradle related inspections when Gradle plugin is disabled
- [`KT-31349`](https://youtrack.jetbrains.com/issue/KT-31349) Add name to argument should not be suggested for Java library classes
- [`KT-31404`](https://youtrack.jetbrains.com/issue/KT-31404) Redundant 'requireNotNull' or 'checkNotNull' inspection: don't remove first argument
- [`KT-25465`](https://youtrack.jetbrains.com/issue/KT-25465) "Redundant 'suspend' modifier" with suspend operator invoke
- [`KT-26337`](https://youtrack.jetbrains.com/issue/KT-26337) Exception (resource not found) in quick-fix tests in AS32
- [`KT-30879`](https://youtrack.jetbrains.com/issue/KT-30879) False positive "Redundant qualifier name"
- [`KT-31415`](https://youtrack.jetbrains.com/issue/KT-31415) UI hangs due to long computations for "Use property access syntax" intention with new inference
- [`KT-31441`](https://youtrack.jetbrains.com/issue/KT-31441) False positive "Remove explicit type arguments" inspection for projection type
- [`KT-30970`](https://youtrack.jetbrains.com/issue/KT-30970) No warning for empty `if` operator and `also`method
- [`KT-31855`](https://youtrack.jetbrains.com/issue/KT-31855) IDE + new inference: Java SAM conversion is not suggested by IDE services

### IDE. JS

- [`KT-31895`](https://youtrack.jetbrains.com/issue/KT-31895) New Project wizard: Kotlin Gradle + Kotlin/JS for Node.js: incorrect DSL is inserted

### IDE. Libraries

- [`KT-30790`](https://youtrack.jetbrains.com/issue/KT-30790) Unstable IDE navigation behavior to `expect`/`actual` symbols in stdlib
- [`KT-30821`](https://youtrack.jetbrains.com/issue/KT-30821) K/N: Navigation downwards the hierarchy in stdlib source code opens to stubs

### IDE. Misc

- [`KT-31364`](https://youtrack.jetbrains.com/issue/KT-31364) IntelliJ routinely hangs and spikes CPU / Memory usage when editing kotlin files

### IDE. Navigation

- [`KT-18322`](https://youtrack.jetbrains.com/issue/KT-18322) Find Usages not finding Java usage of @JvmField declared in primary constructor
- [`KT-27332`](https://youtrack.jetbrains.com/issue/KT-27332) Gutter icons are still shown even if disabled

### IDE. Refactorings

- [`KT-30471`](https://youtrack.jetbrains.com/issue/KT-30471) Make `KotlinElementActionsFactory.createChangeParametersActions` able to just add parameters

### IDE. Run Configurations

- [`KT-29352`](https://youtrack.jetbrains.com/issue/KT-29352) Kotlin + Java 11 + Windows : impossible to run applications with long command lines, even with dynamic.classpath=true

### IDE. Scratch

- [`KT-29642`](https://youtrack.jetbrains.com/issue/KT-29642) Once hidden, `Scratch Output` window wouldn't show the results unless the project is reopened

### IDE. Script

- [`KT-30295`](https://youtrack.jetbrains.com/issue/KT-30295) Resolver for 'completion/highlighting in  ScriptDependenciesSourceInfo...' does not know how to resolve [] or [Library(null)]
- [`KT-30690`](https://youtrack.jetbrains.com/issue/KT-30690) Highlighting for scripts in diff view doesn't work for left part
- [`KT-31452`](https://youtrack.jetbrains.com/issue/KT-31452) IDE editor: MISSING_SCRIPT_STANDARD_TEMPLATE is reported inconsistently with the single line in script

### IDE. Tests Support

- [`KT-30995`](https://youtrack.jetbrains.com/issue/KT-30995) Gradle test runner: "No tasks available" for a test class in non-MPP project

### IDE. Ultimate

- [`KT-30886`](https://youtrack.jetbrains.com/issue/KT-30886) KotlinIdeaResolutionException in Velocity template (.ft) with Kotlin code

### IDE. Wizards

- [`KT-30645`](https://youtrack.jetbrains.com/issue/KT-30645) Update New Project Wizard templates related to Kotlin/JS
- [`KT-31099`](https://youtrack.jetbrains.com/issue/KT-31099) Remove Gradle configuration boilerplate for JS from multiplatform New Project Wizard templates related to Kotlin/JS
- [`KT-31695`](https://youtrack.jetbrains.com/issue/KT-31695) Gradle, JS: update wizard templates

### JS. Tools

- [`KT-31563`](https://youtrack.jetbrains.com/issue/KT-31563) Gradle/JS: npmResolve fails with "Invalid version" when user project's version does not match npm rules
- [`KT-31566`](https://youtrack.jetbrains.com/issue/KT-31566) Gradle/JS: with explicit call to `nodejs { testTask { useNodeJs() } }` configuration fails : "Could not find which method to invoke"
- [`KT-31560`](https://youtrack.jetbrains.com/issue/KT-31560) Gradle: provide descriptions for JS tasks
- [`KT-31564`](https://youtrack.jetbrains.com/issue/KT-31564) Gradle/JS: npmResolve reports warning "karma-webpack@3.0.5 has unmet peer dependency"
- [`KT-31662`](https://youtrack.jetbrains.com/issue/KT-31662) Gradle/JS: with empty `useKarma {}` lambda the execution of `jsBrowserTest` never stops
- [`KT-31686`](https://youtrack.jetbrains.com/issue/KT-31686) Gradle/JS: useKarma { useConfigDirectory() } fails to configure
- [`KT-31694`](https://youtrack.jetbrains.com/issue/KT-31694) Gradle, NPM, windows: creating symlink requires administrator privilege
- [`KT-31931`](https://youtrack.jetbrains.com/issue/KT-31931) Gradle JS or Native: test processing fails in some cases

### JavaScript

- [`KT-31007`](https://youtrack.jetbrains.com/issue/KT-31007) Kotlin/JS 1.3.30 - private method in an interface in the external library causes ReferenceError

### Libraries

- [`KT-30174`](https://youtrack.jetbrains.com/issue/KT-30174) Annotation for experimental stdlib API
- [`KT-30451`](https://youtrack.jetbrains.com/issue/KT-30451) Redundant call of selector in maxBy&minBy
- [`KT-30560`](https://youtrack.jetbrains.com/issue/KT-30560) Fix Throwable::addSuppressed from stdlib to make it work without stdlib-jdk7 in runtime
- [`KT-24810`](https://youtrack.jetbrains.com/issue/KT-24810) Support common string<->ByteArray UTF-8 conversion
- [`KT-29265`](https://youtrack.jetbrains.com/issue/KT-29265) String.toCharArray() is not available in common stdlib
- [`KT-31194`](https://youtrack.jetbrains.com/issue/KT-31194) assertFails and assertFailsWith don't work with suspend functions
- [`KT-31639`](https://youtrack.jetbrains.com/issue/KT-31639) 'Iterbale.drop' drops too much because of overflow
- [`KT-28933`](https://youtrack.jetbrains.com/issue/KT-28933) capitalize() with Locale argument in the JDK stdlib

### Reflection

- [`KT-29041`](https://youtrack.jetbrains.com/issue/KT-29041) KAnnotatedElement should have an extension function to verify if certain annotation is present
- [`KT-30344`](https://youtrack.jetbrains.com/issue/KT-30344) Avoid using .kotlin_module in kotlin-reflect

### Tools. Android Extensions

- [`KT-30993`](https://youtrack.jetbrains.com/issue/KT-30993) Android Extensions: Make @Parcelize functionality non-experimental

### Tools. CLI

- [`KT-27638`](https://youtrack.jetbrains.com/issue/KT-27638) Add -Xjava-sources compiler argument to specify directories with .java source files which can be referenced from the compiled Kotlin sources
- [`KT-27778`](https://youtrack.jetbrains.com/issue/KT-27778) Add -Xpackage-prefix compiler argument to specify package prefix for Java sources resolution
- [`KT-30973`](https://youtrack.jetbrains.com/issue/KT-30973) Compilation on IBM J9 (build 2.9, JRE 1.8.0 AIX ppc64-64-Bit) fails unless -Xuse-javac is specified

### Tools. Compiler Plugins

- [`KT-30343`](https://youtrack.jetbrains.com/issue/KT-30343) Add new Quarkus preset to all-open compiler plugin

### Tools. Gradle

#### New Features

- [`KT-20156`](https://youtrack.jetbrains.com/issue/KT-20156) Publish the Kotlin Javascript Gradle plugin to the Gradle Plugins Portal
- [`KT-26256`](https://youtrack.jetbrains.com/issue/KT-26256) In new MPP, support Java compilation in JVM targets
- [`KT-27273`](https://youtrack.jetbrains.com/issue/KT-27273) Support the Gradle 'application' plugin in new MPP or provide an alternative
- [`KT-30528`](https://youtrack.jetbrains.com/issue/KT-30528) Gradle, JS tests: support basic builtin test runner
- [`KT-31015`](https://youtrack.jetbrains.com/issue/KT-31015) Gradle, JS: Change default for new kotlin-js and experimental kotlin-multiplatform plugins
- [`KT-30573`](https://youtrack.jetbrains.com/issue/KT-30573) Gradle, JS: enable source maps by default, change paths relative to node_modules directory
- [`KT-30747`](https://youtrack.jetbrains.com/issue/KT-30747) Gradle, JS tests: provide option to disable test configuration per target
- [`KT-31010`](https://youtrack.jetbrains.com/issue/KT-31010) Gradle, JS tests: Mocha
- [`KT-31011`](https://youtrack.jetbrains.com/issue/KT-31011) Gradle, JS tests: Karma
- [`KT-31013`](https://youtrack.jetbrains.com/issue/KT-31013) Gradle, JS: Webpack
- [`KT-31016`](https://youtrack.jetbrains.com/issue/KT-31016) Gradle: yarn downloading
- [`KT-31017`](https://youtrack.jetbrains.com/issue/KT-31017) Gradle, yarn: support workspaces

#### Fixes

- [`KT-13256`](https://youtrack.jetbrains.com/issue/KT-13256) CompileJava tasks in Kotlin2Js Gradle plugin
- [`KT-16355`](https://youtrack.jetbrains.com/issue/KT-16355) Rename "compileKotlin2Js" Gradle task to "compileKotlinJs"
- [`KT-26255`](https://youtrack.jetbrains.com/issue/KT-26255) Using the jvmWithJava preset in new MPP leads to counter-intuitive source set names and directory structure
- [`KT-27640`](https://youtrack.jetbrains.com/issue/KT-27640) Do not use `-Xbuild-file` when invoking the Kotlin compiler in Gradle plugins
- [`KT-29284`](https://youtrack.jetbrains.com/issue/KT-29284) kotlin2js plugin applies java plugin
- [`KT-30132`](https://youtrack.jetbrains.com/issue/KT-30132) Could not initialize class org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil on build by gradle
- [`KT-30596`](https://youtrack.jetbrains.com/issue/KT-30596) Kotlin Gradle Plugin: Forward stdout and stderr logger of out of process though gradle logger
- [`KT-31106`](https://youtrack.jetbrains.com/issue/KT-31106) Kotlin compilation fails with locked build script dependencies and Gradle 5
- [`KT-28985`](https://youtrack.jetbrains.com/issue/KT-28985) Java tests not executed in a module created with presets.jvmWithJava
- [`KT-30340`](https://youtrack.jetbrains.com/issue/KT-30340) kotlin("multiplatform") plugin is not working properly with Spring Boot
- [`KT-30784`](https://youtrack.jetbrains.com/issue/KT-30784) Deprecation warning "API 'variant.getPackageLibrary()' is obsolete and has been replaced with 'variant.getPackageLibraryProvider()'" for a multiplatform library with Android target
- [`KT-31027`](https://youtrack.jetbrains.com/issue/KT-31027) java.lang.NoSuchMethodError: No static method hashCode(Z)I in class Ljava/lang/Boolean; or its super classes (declaration of 'java.lang.Boolean' appears in /system/framework/core-libart.jar)
- [`KT-31696`](https://youtrack.jetbrains.com/issue/KT-31696) Gradle, NPM: select one version between tools and all of compile configurations
- [`KT-31697`](https://youtrack.jetbrains.com/issue/KT-31697) Gradle, NPM: report about clashes in packages_imported
- [`KT-31891`](https://youtrack.jetbrains.com/issue/KT-31891) Gradle: JS or Native tests execution: `build --scan` fails with ISE "Expected attachment of type ... but did not find it"
- [`KT-31023`](https://youtrack.jetbrains.com/issue/KT-31023) Update Gradle module metadata warning in MPP publishing

### Tools. Incremental Compile

- [`KT-31131`](https://youtrack.jetbrains.com/issue/KT-31131) Regression: incremental compilation of multi-file part throws exception

### Tools. J2K

- [`KT-23023`](https://youtrack.jetbrains.com/issue/KT-23023) J2K: Inspection to convert Arrays.copyOf(a, size) to a.copyOf(size)
- [`KT-26550`](https://youtrack.jetbrains.com/issue/KT-26550) J2K: Check context/applicability of conversion, don't suggest for libraries, jars, etc.
- [`KT-29568`](https://youtrack.jetbrains.com/issue/KT-29568) Disabled "Convert Java File to Kotlin File" action is shown in project view context menu for XML files

### Tools. JPS

- [`KT-13563`](https://youtrack.jetbrains.com/issue/KT-13563) Kotlin jps-plugin should allow to instrument bytecode from Intellij IDEA.

### Tools. REPL

- [`KT-21443`](https://youtrack.jetbrains.com/issue/KT-21443) Kotlin's JSR223 script engine does not work when used by a fat jar 

### Tools. Scripts

- [`KT-30986`](https://youtrack.jetbrains.com/issue/KT-30986) Missing dependencies when JSR-223 script engines are used from `kotlin-script-util`

### Tools. kapt

- [`KT-26203`](https://youtrack.jetbrains.com/issue/KT-26203) `kapt.use.worker.api=true` throws a NullPointerException on Java 10/11
- [`KT-30739`](https://youtrack.jetbrains.com/issue/KT-30739) Kapt generated sources are not visible from the IDE when "Create separate module per source set" is disabled
- [`KT-31064`](https://youtrack.jetbrains.com/issue/KT-31064) Periodically build crash when using incremental kapt
- [`KT-23880`](https://youtrack.jetbrains.com/issue/KT-23880) Kapt: Support incremental annotation processors
- [`KT-31322`](https://youtrack.jetbrains.com/issue/KT-31322) Kapt does not run annotation processing when sources change.
- [`KT-30979`](https://youtrack.jetbrains.com/issue/KT-30979) Issue with Dagger2 providers MissingBinding with 1.3.30
- [`KT-31127`](https://youtrack.jetbrains.com/issue/KT-31127) Kotlin-generating processor which uses Filer API breaks JavaCompile task
- [`KT-31714`](https://youtrack.jetbrains.com/issue/KT-31714) incremental kapt: FileSystemException: Too many open files

## 1.3.31

### Compiler

#### Fixes

- [`KT-26418`](https://youtrack.jetbrains.com/issue/KT-26418) Back-end (JVM) Internal error when compiling decorated suspend inline functions
- [`KT-26925`](https://youtrack.jetbrains.com/issue/KT-26925) Decorated suspend inline function continuation resumes in wrong spot
- [`KT-30706`](https://youtrack.jetbrains.com/issue/KT-30706) Passing noinline lambda as (cross)inline parameter result in wrong state-machine
- [`KT-30707`](https://youtrack.jetbrains.com/issue/KT-30707) Java interop of coroutines inside inline functions is broken
- [`KT-30997`](https://youtrack.jetbrains.com/issue/KT-30997) Crash with suspend crossinline

### IDE. Inspections and Intentions

- [`KT-30879`](https://youtrack.jetbrains.com/issue/KT-30879) False positive "Redundant qualifier name"
- [`KT-31112`](https://youtrack.jetbrains.com/issue/KT-31112) "Remove redundant qualifier name" inspection false positive for property with irrelevant import

### JavaScript

- [`KT-31007`](https://youtrack.jetbrains.com/issue/KT-31007) Kotlin/JS 1.3.30 - private method in an interface in the external library causes ReferenceError

### Tools. Gradle

- [`KT-31027`](https://youtrack.jetbrains.com/issue/KT-31027) java.lang.NoSuchMethodError: No static method hashCode(Z)I in class Ljava/lang/Boolean; or its super classes (declaration of 'java.lang.Boolean' appears in /system/framework/core-libart.jar)

### Tools. kapt

- [`KT-30979`](https://youtrack.jetbrains.com/issue/KT-30979) Issue with Dagger2 providers MissingBinding with 1.3.30

## 1.3.30

### Compiler

#### New Features

- [`KT-19664`](https://youtrack.jetbrains.com/issue/KT-19664) Allow more permissive visibility for non-virtual actual declarations
- [`KT-29586`](https://youtrack.jetbrains.com/issue/KT-29586) Add support for Android platform annotations
- [`KT-29604`](https://youtrack.jetbrains.com/issue/KT-29604) Do not implicitly propagate deprecations originated in Java

#### Performance Improvements

- [`KT-24876`](https://youtrack.jetbrains.com/issue/KT-24876) Emit calls to java.lang.Long.divideUnsigned for unsigned types when target version is 1.8
- [`KT-25974`](https://youtrack.jetbrains.com/issue/KT-25974) 'when' by unsigned integers is not translated to tableswitch/lookupswitch
- [`KT-28015`](https://youtrack.jetbrains.com/issue/KT-28015) Coroutine state-machine shall use Result.throwOnFailure
- [`KT-29229`](https://youtrack.jetbrains.com/issue/KT-29229) Intrinsify 'in' operator for unsigned integer ranges
- [`KT-29230`](https://youtrack.jetbrains.com/issue/KT-29230) Specialize 'next' method call for unsigned integer range and progression iterators

#### Fixes

- [`KT-7185`](https://youtrack.jetbrains.com/issue/KT-7185) Parse import directives in the middle of the file, report a diagnostic instead
- [`KT-7237`](https://youtrack.jetbrains.com/issue/KT-7237) Parser recovery (angle bracket mismatch)
- [`KT-11656`](https://youtrack.jetbrains.com/issue/KT-11656) Could not generate LightClass because of ISE from bridge generation on invalid code
- [`KT-13497`](https://youtrack.jetbrains.com/issue/KT-13497) Better recovery in comma-separated lists in case of missing comma
- [`KT-13703`](https://youtrack.jetbrains.com/issue/KT-13703) Restore parser better when `class` is missing from `enum` declaration
- [`KT-13731`](https://youtrack.jetbrains.com/issue/KT-13731) Recover parser on value parameter without a type
- [`KT-14227`](https://youtrack.jetbrains.com/issue/KT-14227) Incorrect code is generated when using MutableMap.set with plusAssign operator
- [`KT-19389`](https://youtrack.jetbrains.com/issue/KT-19389) Couldn't inline method call 'with'
- [`KT-20065`](https://youtrack.jetbrains.com/issue/KT-20065) "Cannot serialize error type: [ERROR : Unknown type parameter 0]" with generic typealias
- [`KT-20322`](https://youtrack.jetbrains.com/issue/KT-20322) Debug: member value returned from suspending function is not updated immediately 
- [`KT-20780`](https://youtrack.jetbrains.com/issue/KT-20780) "Cannot serialize error type: [ERROR : Unknown type parameter 0]" with parameterized inner type alias
- [`KT-21405`](https://youtrack.jetbrains.com/issue/KT-21405) Throwable “Rewrite at slice LEXICAL_SCOPE key: VALUE_PARAMETER_LIST” on editing string literal in kotlin-js module
- [`KT-21775`](https://youtrack.jetbrains.com/issue/KT-21775) "Cannot serialize error type: [ERROR : Unknown type parameter 0]" with typealias used from a different module
- [`KT-22818`](https://youtrack.jetbrains.com/issue/KT-22818) "UnsupportedOperationException: Don't know how to generate outer expression" on using non-trivial expression in default argument of `expect` function
- [`KT-23117`](https://youtrack.jetbrains.com/issue/KT-23117) Local delegate + local object = NoSuchMethodError
- [`KT-23701`](https://youtrack.jetbrains.com/issue/KT-23701) Report error when -Xmultifile-parts-inherit is used and relevant JvmMultifileClass parts have any state
- [`KT-23992`](https://youtrack.jetbrains.com/issue/KT-23992) Target prefixes for annotations on supertype list elements are not checked
- [`KT-24490`](https://youtrack.jetbrains.com/issue/KT-24490) Wrong type is inferred when last expression in lambda has functional type
- [`KT-24871`](https://youtrack.jetbrains.com/issue/KT-24871) Optimize iteration and contains for UIntRange/ULongRange
- [`KT-24964`](https://youtrack.jetbrains.com/issue/KT-24964) "Cannot serialize error type: [ERROR : Unknown type parameter 0]" with `Validated` typealias from Arrow
- [`KT-25383`](https://youtrack.jetbrains.com/issue/KT-25383) Named function as last statement in lambda doesn't coerce to Unit
- [`KT-25431`](https://youtrack.jetbrains.com/issue/KT-25431) Type mismatch when trying to bind mutable property with complex common system
- [`KT-25435`](https://youtrack.jetbrains.com/issue/KT-25435) Try/catch as the last expression of lambda cause type mismatch
- [`KT-25437`](https://youtrack.jetbrains.com/issue/KT-25437) Type variable fixation of postponed arguments and type variables with Nothing constraint
- [`KT-25446`](https://youtrack.jetbrains.com/issue/KT-25446) Empty labeled return doesn't force coercion to Unit
- [`KT-26069`](https://youtrack.jetbrains.com/issue/KT-26069) NoSuchMethodError on calling remove/getOrDefault on a Kotlin subclass of Java subclass of Map
- [`KT-26638`](https://youtrack.jetbrains.com/issue/KT-26638) Check for repeatablilty of annotations doesn't take into account annotations with use-site target
- [`KT-26816`](https://youtrack.jetbrains.com/issue/KT-26816) Lambdas to Nothing is inferred if multilevel collections is used (listOf, mapOf, etc)
- [`KT-27190`](https://youtrack.jetbrains.com/issue/KT-27190) State machine elimination after inlining stopped working (regression)
- [`KT-27241`](https://youtrack.jetbrains.com/issue/KT-27241) Contracts: smartcasts don't work correctly if type checking for contract function is used
- [`KT-27565`](https://youtrack.jetbrains.com/issue/KT-27565) Lack of fallback resolution for SAM conversions for Kotlin functions in new inference
- [`KT-27799`](https://youtrack.jetbrains.com/issue/KT-27799) Prohibit references to reified type parameters in annotation arguments in local classes / anonymous objects
- [`KT-28182`](https://youtrack.jetbrains.com/issue/KT-28182) Kotlin Bytecode tool window shows incorrect output on annotated property with backing field
- [`KT-28236`](https://youtrack.jetbrains.com/issue/KT-28236) "Cannot serialize error type: [ERROR : Unknown type parameter 2]" with inferred type arguments in generic extension function from Arrow
- [`KT-28309`](https://youtrack.jetbrains.com/issue/KT-28309) Do not generate LVT entries with different types pointing to the same slot, but have different types
- [`KT-28317`](https://youtrack.jetbrains.com/issue/KT-28317) Strange behavior in testJvmAssertInlineFunctionAssertionsEnabled on Jdk 6 and exception on JDK 8
- [`KT-28453`](https://youtrack.jetbrains.com/issue/KT-28453) Mark anonymous classes for callable references as synthetic
- [`KT-28598`](https://youtrack.jetbrains.com/issue/KT-28598) Type is inferred incorrectly to Any on a deep generic type with out projection
- [`KT-28654`](https://youtrack.jetbrains.com/issue/KT-28654) No report about type mismatch inside a lambda in generic functions with a type parameter as a return type
- [`KT-28670`](https://youtrack.jetbrains.com/issue/KT-28670) Not null smartcasts on an intersection of nullable types don't work
- [`KT-28718`](https://youtrack.jetbrains.com/issue/KT-28718) progressive mode plus new inference result in different floating-point number comparisons
- [`KT-28810`](https://youtrack.jetbrains.com/issue/KT-28810) Suspend function's continuation parameter is missing from LVT
- [`KT-28855`](https://youtrack.jetbrains.com/issue/KT-28855) NoSuchMethodError with vararg of unsigned Int in generic class constructor
- [`KT-28984`](https://youtrack.jetbrains.com/issue/KT-28984) Exception when subtype of kotlin.Function is used as an expected one for lambda or callable reference
- [`KT-28993`](https://youtrack.jetbrains.com/issue/KT-28993) Incorrect behavior when two lambdas are passed outside a parenthesized argument list
- [`KT-29144`](https://youtrack.jetbrains.com/issue/KT-29144) Interface with companion object generates invalid bytecode in progressive mode
- [`KT-29228`](https://youtrack.jetbrains.com/issue/KT-29228) Intrinsify 'for' loop for unsigned integer ranges and progressions
- [`KT-29324`](https://youtrack.jetbrains.com/issue/KT-29324) Warnings indexing jdk 11 classes
- [`KT-29367`](https://youtrack.jetbrains.com/issue/KT-29367) New inference doesn't wrap annotated type from java to TypeWithEnhancement
- [`KT-29507`](https://youtrack.jetbrains.com/issue/KT-29507) @field-targeted annotation on property with both getter and setter is absent from bytecode
- [`KT-29705`](https://youtrack.jetbrains.com/issue/KT-29705) 'Rewrite at slice CONSTRUCTOR` of JS class while editing another JVM-class
- [`KT-29792`](https://youtrack.jetbrains.com/issue/KT-29792) UnsupportedOperationException: Unsupported annotation argument type when using Java annotation with infinity or NaN as a default value
- [`KT-29891`](https://youtrack.jetbrains.com/issue/KT-29891) Kotlin doesn't allow to use local class literals as annotation arguments
- [`KT-29912`](https://youtrack.jetbrains.com/issue/KT-29912) Crossinline nonsuspend lambda leads to KNPE during inlining
- [`KT-29965`](https://youtrack.jetbrains.com/issue/KT-29965) Don't generate annotation on $default  method
- [`KT-30030`](https://youtrack.jetbrains.com/issue/KT-30030) Extensive 'Rewrite at slice'-exception with contracts in JS module of multiplatform project
- [`KT-22043`](https://youtrack.jetbrains.com/issue/KT-22043) Report an error when comparing enum (==/!=/when) to any other incompatible type since 1.4
- [`KT-26150`](https://youtrack.jetbrains.com/issue/KT-26150) KotlinFrontendException is thrown when callsInPlace called twice with different InvocationKind in functions with contracts
- [`KT-26153`](https://youtrack.jetbrains.com/issue/KT-26153) Contract is allowed when it's at the beginning in control flow terms, but not in tokens order terms (contract doesn't work)
- [`KT-26191`](https://youtrack.jetbrains.com/issue/KT-26191) Contract may not be the first statement if it's part of the expression
- [`KT-29178`](https://youtrack.jetbrains.com/issue/KT-29178) Prohibit arrays of reified type parameters in annotation arguments in local classes / anonymous objects
- [`KT-20507`](https://youtrack.jetbrains.com/issue/KT-20507) PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL not reported for generic base class constructor call, IAE at run-time
- [`KT-20849`](https://youtrack.jetbrains.com/issue/KT-20849) Inference results in Nothing type argument in case of passing 'out T' to 'in T1'
- [`KT-28285`](https://youtrack.jetbrains.com/issue/KT-28285) NullPointerException on calling Array constructor compiled via Excelsior JET
- [`KT-29376`](https://youtrack.jetbrains.com/issue/KT-29376) Report a deprecation warning when comparing enum to any other incompatible type
- [`KT-29884`](https://youtrack.jetbrains.com/issue/KT-29884) Report warning on @Synchronized on inline method
- [`KT-30073`](https://youtrack.jetbrains.com/issue/KT-30073) ClassCastException on coroutine start with crossinline lambda
- [`KT-30597`](https://youtrack.jetbrains.com/issue/KT-30597) "Extend selection" throws exception in empty class body case
- [`KT-29492`](https://youtrack.jetbrains.com/issue/KT-29492) Double cross-inline of suspending functions produces incorrect code
- [`KT-30508`](https://youtrack.jetbrains.com/issue/KT-30508) Wrong file name in metadata of suspend function capturing crossinline lambda
- [`KT-30679`](https://youtrack.jetbrains.com/issue/KT-30679) "KotlinFrontEndException: Front-end Internal error: Failed to analyze declaration" exception during a compilation of a multiplatform project containing Kotlin Script File

### IDE

#### New Features

- [`KT-26950`](https://youtrack.jetbrains.com/issue/KT-26950) Support Multiline TODO comments
- [`KT-29034`](https://youtrack.jetbrains.com/issue/KT-29034) Make JvmDeclarationSearch find private fields in kotlin classes

#### Performance Improvements

- [`KT-29457`](https://youtrack.jetbrains.com/issue/KT-29457) FindImplicitNothingAction#update freezes UI for 30 secs
- [`KT-29551`](https://youtrack.jetbrains.com/issue/KT-29551) CreateKotlinSdkActivity runs on UI thread

#### Fixes

- [`KT-11143`](https://youtrack.jetbrains.com/issue/KT-11143) Do not insert closing brace for string template between open brace and identifier
- [`KT-18503`](https://youtrack.jetbrains.com/issue/KT-18503) Optimize imports produces red code
- [`KT-27283`](https://youtrack.jetbrains.com/issue/KT-27283) KotlinULiteralExpression and PsiLanguageInjectionHost mismatch
- [`KT-27794`](https://youtrack.jetbrains.com/issue/KT-27794) KotlinAnnotatedElementsSearcher doesn't process method parameters
- [`KT-28272`](https://youtrack.jetbrains.com/issue/KT-28272) UAST: Need to be able to identify SAM conversions
- [`KT-28360`](https://youtrack.jetbrains.com/issue/KT-28360) Getting tons of "There are 2 classes with same fqName" logs in IntelliJ
- [`KT-28739`](https://youtrack.jetbrains.com/issue/KT-28739) Bad caret position after `Insert curly braces around variable` inspection
- [`KT-29013`](https://youtrack.jetbrains.com/issue/KT-29013) Injection with interpolation loses suffix
- [`KT-29025`](https://youtrack.jetbrains.com/issue/KT-29025) Implement `UReferenceExpression.referenceNameElement` for Kotlin
- [`KT-29287`](https://youtrack.jetbrains.com/issue/KT-29287) Exception in ultra-light classes on method annotated with @Throws
- [`KT-29381`](https://youtrack.jetbrains.com/issue/KT-29381) Highlight return lambda expressions when cursor is one the call with lambda argument
- [`KT-29434`](https://youtrack.jetbrains.com/issue/KT-29434) Can not detect injection host in string passed as argument into arrayOf() function
- [`KT-29464`](https://youtrack.jetbrains.com/issue/KT-29464) Project reopening does not create missing Kotlin SDK for Native modules (like it does for other non-JVM ones)
- [`KT-29467`](https://youtrack.jetbrains.com/issue/KT-29467) Maven/Gradle re-import does not add missing Kotlin SDK for kotlin2js modules (non-MPP JavaScript)
- [`KT-29804`](https://youtrack.jetbrains.com/issue/KT-29804) Probable error in the "Kotlin (Mobile Android/iOS)" new project template in IntelliJ
- [`KT-30033`](https://youtrack.jetbrains.com/issue/KT-30033) UAST: Delegation expression missing from parse tree
- [`KT-30388`](https://youtrack.jetbrains.com/issue/KT-30388) Disable constant exception reporting from release versions
- [`KT-30524`](https://youtrack.jetbrains.com/issue/KT-30524) "java.lang.IllegalStateException: This method shouldn't be invoked for LOCAL visibility" on add import
- [`KT-30534`](https://youtrack.jetbrains.com/issue/KT-30534) KotlinUObjectLiteralExpression returns classReference whose referenceNameElement is null
- [`KT-30546`](https://youtrack.jetbrains.com/issue/KT-30546) Kotlin UImportStatement's children references always resolve to null
- [`KT-5435`](https://youtrack.jetbrains.com/issue/KT-5435) Surround with try/catch should generate more Kotlin-style code

### IDE. Android

- [`KT-29847`](https://youtrack.jetbrains.com/issue/KT-29847) Many IDEA plugins are not loaded in presence of Kotlin plugin: "Plugins should not have cyclic dependencies"

### IDE. Code Style, Formatting

- [`KT-23295`](https://youtrack.jetbrains.com/issue/KT-23295) One-line comment indentation in functions with expression body 
- [`KT-28905`](https://youtrack.jetbrains.com/issue/KT-28905) When is "... if long" hitting?
- [`KT-29304`](https://youtrack.jetbrains.com/issue/KT-29304) Settings / Code Style / Kotlin mentions "methods" instead of functions
- [`KT-26954`](https://youtrack.jetbrains.com/issue/KT-26954) Bad indentation for single function with expression body in new code style

### IDE. Completion

- [`KT-18663`](https://youtrack.jetbrains.com/issue/KT-18663) Support "smart enter/complete statement" completion for method calls
- [`KT-28394`](https://youtrack.jetbrains.com/issue/KT-28394) Improve code completion for top level class/interface to incorporate filename
- [`KT-29435`](https://youtrack.jetbrains.com/issue/KT-29435) org.jetbrains.kotlin.types.TypeUtils.contains hanging forever and freezing IntelliJ
- [`KT-27915`](https://youtrack.jetbrains.com/issue/KT-27915) Stop auto-completing braces for companion objects

### IDE. Debugger

- [`KT-22250`](https://youtrack.jetbrains.com/issue/KT-22250) Evaluate: 'this' shows different values when evaluated as a variable/watch
- [`KT-24829`](https://youtrack.jetbrains.com/issue/KT-24829) Access to coroutineContext in 'Evaluate expression'
- [`KT-25220`](https://youtrack.jetbrains.com/issue/KT-25220) Evaluator: a instance of Pair returned instead of String ("Extract function" failed)
- [`KT-25222`](https://youtrack.jetbrains.com/issue/KT-25222) Evaluate: ClassCastException: ObjectValue cannot be cast to IntValue ("Extract function" failed)
- [`KT-26913`](https://youtrack.jetbrains.com/issue/KT-26913) Change local variable name mangling ($receiver -> this_<label>)
- [`KT-28087`](https://youtrack.jetbrains.com/issue/KT-28087) [Kotlin/JVM view] Inconsistent debugging data inside forEachIndexed
- [`KT-28134`](https://youtrack.jetbrains.com/issue/KT-28134) Separate JVM/Kotlin views in "Variables" tool window
- [`KT-28192`](https://youtrack.jetbrains.com/issue/KT-28192) Exception from KotlinEvaluator: cannot find local variable
- [`KT-28680`](https://youtrack.jetbrains.com/issue/KT-28680) Missing `this` word completion in "Evaluate expression" window
- [`KT-28728`](https://youtrack.jetbrains.com/issue/KT-28728) Async stack trace support for Kotlin coroutines
- [`KT-21650`](https://youtrack.jetbrains.com/issue/KT-21650) Debugger: Can't evaluate value, resolution error
- [`KT-23828`](https://youtrack.jetbrains.com/issue/KT-23828) Debugger: "Smart cast is impossible" when evaluating expression
- [`KT-29661`](https://youtrack.jetbrains.com/issue/KT-29661) Evaluate expression: "Cannot find local variable" for variable name escaped with backticks
- [`KT-29814`](https://youtrack.jetbrains.com/issue/KT-29814) Can't evaluate a property on star-projected type
- [`KT-29871`](https://youtrack.jetbrains.com/issue/KT-29871) Debugger in IDE does not handle correctly extensions.
- [`KT-30182`](https://youtrack.jetbrains.com/issue/KT-30182) Incorrect KT elvis expression debugger evaluation
- [`KT-29189`](https://youtrack.jetbrains.com/issue/KT-29189) [BE] 'Step Over' falls through 'return when' (and 'return if') instead of executing individual branches
- [`KT-29234`](https://youtrack.jetbrains.com/issue/KT-29234) ISE “@NotNull method org/jetbrains/kotlin/codegen/binding/CodegenBinding.anonymousClassForCallable must not return null” on debugging with breakpoints in Kotlin script file
- [`KT-29423`](https://youtrack.jetbrains.com/issue/KT-29423) Unable to evaluate lambdas on jdk 9-11
- [`KT-30220`](https://youtrack.jetbrains.com/issue/KT-30220) Empty variables view when breakpoint inside an lambda inside class
- [`KT-30318`](https://youtrack.jetbrains.com/issue/KT-30318) KotlinCoroutinesAsyncStackTraceProvider slows down java debugging
- [`KT-17811`](https://youtrack.jetbrains.com/issue/KT-17811) Couldn't inline method error for inline method with anonymous object initialization and reified type parameter
- [`KT-30611`](https://youtrack.jetbrains.com/issue/KT-30611) Debugger: in projects with stdlib of 1.2.n version Frames view can't complete loading, EvaluateException: "Method threw 'java.lang.ClassNotFoundException' exception." at EvaluateExceptionUtil.createEvaluateException()

### IDE. Decompiler

- [`KT-9618`](https://youtrack.jetbrains.com/issue/KT-9618) Exception in ClassClsStubBuilder.createNestedClassStub() while opening recent project
- [`KT-29427`](https://youtrack.jetbrains.com/issue/KT-29427) Exception in ClassClsStubBuilder.createNestedClassStub() for obfuscated library

### IDE. Gradle

- [`KT-26865`](https://youtrack.jetbrains.com/issue/KT-26865) Gradle build in IDE: error messages in Native sources are not hyperlinks
- [`KT-28515`](https://youtrack.jetbrains.com/issue/KT-28515) Failed to import Kotlin project with gradle 5.0
- [`KT-29564`](https://youtrack.jetbrains.com/issue/KT-29564) kotlin.parallel.tasks.in.project=true causes idea to create kotlin modules with target JVM 1.6
- [`KT-30076`](https://youtrack.jetbrains.com/issue/KT-30076) Memory leaks in Kotlin import
- [`KT-30379`](https://youtrack.jetbrains.com/issue/KT-30379) Gradle 5.3 publishes an MPP with broken Maven scope mapping

### IDE. Gradle. Script

- [`KT-27684`](https://youtrack.jetbrains.com/issue/KT-27684) Gradle Kotlin DSL: the `rootProject` field is unresolved in IDEA for a common module
- [`KT-29465`](https://youtrack.jetbrains.com/issue/KT-29465) IndexNotReadyException on context menu invocation for build.gradle.kts file
- [`KT-29707`](https://youtrack.jetbrains.com/issue/KT-29707) "Navigate declaration" navigates to compiled class in gradle cache folder instead of classes defined in gradle buildSrc folder
- [`KT-29832`](https://youtrack.jetbrains.com/issue/KT-29832) Multiple Script Definitions for settings.gradle.kts
- [`KT-30623`](https://youtrack.jetbrains.com/issue/KT-30623) Errors in build.gradle.kts after applying new script dependencies
- [`KT-29474`](https://youtrack.jetbrains.com/issue/KT-29474) Regression in 1.3.20: Kotlin IDE plugin parses all *.gradle.kts files when any class in buildSrc is opened
- [`KT-30130`](https://youtrack.jetbrains.com/issue/KT-30130) “Access is allowed from event dispatch thread only.” from ScriptNewDependenciesNotificationKt.removeScriptDependenciesNotificationPanel() on creating foo.gradle.kts files in IJ from master

### IDE. Hints

- [`KT-29196`](https://youtrack.jetbrains.com/issue/KT-29196) Variable type hints are redundant for constructor calls of nested classes
- [`KT-30058`](https://youtrack.jetbrains.com/issue/KT-30058) IndexNotReadyException from quick documentation when popup is active

### IDE. Hints. Inlay

- [`KT-19558`](https://youtrack.jetbrains.com/issue/KT-19558) Wrong position of type hint while renaming Kotlin variable
- [`KT-27438`](https://youtrack.jetbrains.com/issue/KT-27438) "Show lambda return expression hints" breaks code indentation
- [`KT-28870`](https://youtrack.jetbrains.com/issue/KT-28870) Rework "Lambda return expression" hint as between_lines_hint of disable it by default

### IDE. Hints. Parameter Info

- [`KT-29574`](https://youtrack.jetbrains.com/issue/KT-29574) Incorrect parameter info popup for lambda nested in object

### IDE. Inspections and Intentions

#### New Features

- [`KT-16118`](https://youtrack.jetbrains.com/issue/KT-16118) "Introduce import alias" intention
- [`KT-17119`](https://youtrack.jetbrains.com/issue/KT-17119) Inspection for (Scala-like) `= { ... }` syntax without expected type in function definition
- [`KT-26128`](https://youtrack.jetbrains.com/issue/KT-26128) Inspection for suspension inside synchronized and withLock functions
- [`KT-27556`](https://youtrack.jetbrains.com/issue/KT-27556) Add intention for collections, !collection.isEmpty() -> collection.isNotEmpty()
- [`KT-27670`](https://youtrack.jetbrains.com/issue/KT-27670) Add quick fix: wrap expression in a lambda if compatible functional type is required
- [`KT-28803`](https://youtrack.jetbrains.com/issue/KT-28803) Inspection: result of enum entries comparison is always false / true
- [`KT-28953`](https://youtrack.jetbrains.com/issue/KT-28953) Add intention to add underscores to decimal numerical literal
- [`KT-29001`](https://youtrack.jetbrains.com/issue/KT-29001) Add intention to move variable declaration before when-expression into when's subject
- [`KT-29113`](https://youtrack.jetbrains.com/issue/KT-29113) Warn about  redundant requireNotNull and checkNotNull usages
- [`KT-29321`](https://youtrack.jetbrains.com/issue/KT-29321) "Remove empty primary constructor": apply for enum entries
- [`KT-12134`](https://youtrack.jetbrains.com/issue/KT-12134) Suggest to remove qualifier in FQN name
- [`KT-17278`](https://youtrack.jetbrains.com/issue/KT-17278) Inspection to replace Java 8 Map.forEach with Kotlin's forEach
- [`KT-26965`](https://youtrack.jetbrains.com/issue/KT-26965) Add inspection + quickfix for replacing Collection<T>.count() with .size
- [`KT-30123`](https://youtrack.jetbrains.com/issue/KT-30123) Add intention to replace isEmpty/isNotEmpty method negation
- [`KT-25272`](https://youtrack.jetbrains.com/issue/KT-25272) Unused expression as last expression of normal function should have quickfix to add "return"
- [`KT-30456`](https://youtrack.jetbrains.com/issue/KT-30456) Improve: intention "Introduce Import Alias" should suggest new names for the new alias.

#### Fixes

- [`KT-7593`](https://youtrack.jetbrains.com/issue/KT-7593) On splitting property declaration for functional expression additional bracket added
- [`KT-12273`](https://youtrack.jetbrains.com/issue/KT-12273) "Replace with operator" intention is suggested for some non-operator functions and produces invalid code
- [`KT-18715`](https://youtrack.jetbrains.com/issue/KT-18715) Replace if with elvis swallows comments
- [`KT-19254`](https://youtrack.jetbrains.com/issue/KT-19254) Intention to convert object literal to class always creates a class named "O"
- [`KT-25501`](https://youtrack.jetbrains.com/issue/KT-25501) "Replace overloaded operator with function call" changes semantics of increment and decrement operators
- [`KT-26979`](https://youtrack.jetbrains.com/issue/KT-26979) "Lambda argument inside parentheses" inspection is not reported, if function type is actual type argument, but not formal parameter type
- [`KT-27143`](https://youtrack.jetbrains.com/issue/KT-27143) Intention "Replace camel-case name with spaces" is suggested for snake_case names in test functions and renames them incorrectly
- [`KT-28081`](https://youtrack.jetbrains.com/issue/KT-28081) "Convert to lambda" changes expression type for interface with multiple supertypes
- [`KT-28131`](https://youtrack.jetbrains.com/issue/KT-28131) False positive "Redundant lambda arrow" with a functional type argument
- [`KT-28224`](https://youtrack.jetbrains.com/issue/KT-28224) "Add braces to 'else' statement" moves comment outside braces when 'if-else' is inside 'if / when' branch
- [`KT-28592`](https://youtrack.jetbrains.com/issue/KT-28592) False positive "Remove redundant backticks" for underscore variable name
- [`KT-28596`](https://youtrack.jetbrains.com/issue/KT-28596) "Can be replaced with binary operator" shouldn't be suggested when receiver or argument is floating point type
- [`KT-28641`](https://youtrack.jetbrains.com/issue/KT-28641) "Remove useless cast" produces a dangling lambda ("Too many arguments" error)
- [`KT-28698`](https://youtrack.jetbrains.com/issue/KT-28698) "Convert to apply" intention: include function calls with `this` passed as an argument
- [`KT-28773`](https://youtrack.jetbrains.com/issue/KT-28773) Kotlin/JS: Wrong inspection to replace .equals() with == on dynamic values
- [`KT-28851`](https://youtrack.jetbrains.com/issue/KT-28851) 'Convert parameter to receiver' adds `Array<out T>` wrapper to `vararg` parameter and drops `override` modifier in implementations
- [`KT-28969`](https://youtrack.jetbrains.com/issue/KT-28969) TYPE_MISMATCH in array vs non-array case: two quick fixes exist for annotation and none of them adds array literal
- [`KT-28995`](https://youtrack.jetbrains.com/issue/KT-28995) "Add parameter to constructor" quickfix for first enum member changes arguments for all members
- [`KT-29051`](https://youtrack.jetbrains.com/issue/KT-29051) "Add parameter to constructor" quickfix for not-first enum member: "PsiInvalidElementAccessException: Element: class org.jetbrains.kotlin.psi.KtStringTemplateExpression #kotlin  because: different providers"
- [`KT-29052`](https://youtrack.jetbrains.com/issue/KT-29052) "Add parameter to constructor" quickfix for not-first enum member inserts FQN type for parameter
- [`KT-29056`](https://youtrack.jetbrains.com/issue/KT-29056) KNPE in ConvertPrimaryConstructorToSecondary with missing property identifier
- [`KT-29085`](https://youtrack.jetbrains.com/issue/KT-29085) False positive "Class member can have 'private' visibility" for a `const val` used in a public inline function
- [`KT-29093`](https://youtrack.jetbrains.com/issue/KT-29093) False positive inspection "Redundant lambda arrow" with nested lambdas
- [`KT-29099`](https://youtrack.jetbrains.com/issue/KT-29099) "Convert to apply" intention is not available for a single function call
- [`KT-29128`](https://youtrack.jetbrains.com/issue/KT-29128) False positive 'Explicitly given type is redundant here' when typealias is used
- [`KT-29153`](https://youtrack.jetbrains.com/issue/KT-29153) False negative "'rangeTo' or the '..' call should be replaced with 'until'" with bracketed expressions
- [`KT-29193`](https://youtrack.jetbrains.com/issue/KT-29193) Quick fix "Create extension function" `List<Int>.set` should not be suggested for read-only collections
- [`KT-29238`](https://youtrack.jetbrains.com/issue/KT-29238) Non-canonical modifiers order inspection incorrectly includes annotations into range
- [`KT-29248`](https://youtrack.jetbrains.com/issue/KT-29248) "Convert member to extension" doesn't preserve visibility
- [`KT-29416`](https://youtrack.jetbrains.com/issue/KT-29416) False positive "Redundant property getter" for `external` getter
- [`KT-29469`](https://youtrack.jetbrains.com/issue/KT-29469) False positive in "Boolean literal argument without parameter name" inspection for varargs parameters
- [`KT-29549`](https://youtrack.jetbrains.com/issue/KT-29549) Make package name convention inspection global
- [`KT-29567`](https://youtrack.jetbrains.com/issue/KT-29567) "Remove empty class body" is a poor name for inspection text
- [`KT-29606`](https://youtrack.jetbrains.com/issue/KT-29606) Do not propose to remove unused parameter of property setter
- [`KT-29763`](https://youtrack.jetbrains.com/issue/KT-29763) False negative "Object literal can be converted to lambda" for block body function with explicit return
- [`KT-30007`](https://youtrack.jetbrains.com/issue/KT-30007) False negative "Add import for '...'" in UserType
- [`KT-19944`](https://youtrack.jetbrains.com/issue/KT-19944) multiplatform: Convert expect/actual function to property should keep the caret on the converted function
- [`KT-27289`](https://youtrack.jetbrains.com/issue/KT-27289) "Create" quick fix on FQN does nothing with KNPE at KotlinRefactoringUtilKt$chooseContainerElement$1.renderText()
- [`KT-29312`](https://youtrack.jetbrains.com/issue/KT-29312) "Make constructor parameter a property" produces wrong modifier order + exception "Invalid range specified"
- [`KT-29414`](https://youtrack.jetbrains.com/issue/KT-29414) "Main parameter is not necessary" inspection reports parameter of `main()` in object
- [`KT-29499`](https://youtrack.jetbrains.com/issue/KT-29499) "Unsafe call of inline function with nullable extension receiver" inspection ignores inferred nullability
- [`KT-29927`](https://youtrack.jetbrains.com/issue/KT-29927) Missing "Import members from" intention with type check operator in `when` branch
- [`KT-30010`](https://youtrack.jetbrains.com/issue/KT-30010) Introduce alternative quick-fixes for `map[key]!!`
- [`KT-30166`](https://youtrack.jetbrains.com/issue/KT-30166) False positive "Redundant companion reference" on companion with the outer class name
- [`KT-14886`](https://youtrack.jetbrains.com/issue/KT-14886) Create Property from Usage should place generated property next to other properties
- [`KT-16139`](https://youtrack.jetbrains.com/issue/KT-16139) Adding explicit type argument leads to type mismatch
- [`KT-19462`](https://youtrack.jetbrains.com/issue/KT-19462) False positive inspection "Redundant lambda arrow" for overloaded functions
- [`KT-22137`](https://youtrack.jetbrains.com/issue/KT-22137) Create class quickfix is not suggested in return statement
- [`KT-23259`](https://youtrack.jetbrains.com/issue/KT-23259) False positive unchecked cast warning/quickfix result in good code turning red
- [`KT-27641`](https://youtrack.jetbrains.com/issue/KT-27641) "Specify type explicitly" suggests too general type even when type hint shows specific generic type
- [`KT-29124`](https://youtrack.jetbrains.com/issue/KT-29124) False positive inspection 'Redundant lambda arrow' with generic function/constructor with lambda argument
- [`KT-29590`](https://youtrack.jetbrains.com/issue/KT-29590) False positive inspection "Redundant lambda arrow" with vararg lambda arguments passed via spread operator
- [`KT-29977`](https://youtrack.jetbrains.com/issue/KT-29977) False positive "Unused import directive" for typealias of an enum imported as static
- [`KT-30233`](https://youtrack.jetbrains.com/issue/KT-30233) Change order of the quick fixes when method does not accept nullable types
- [`KT-30341`](https://youtrack.jetbrains.com/issue/KT-30341) False positive 'Use withIndex() instead of manual index increment' inspection with destructive declaration in 'for' loop
- [`KT-30414`](https://youtrack.jetbrains.com/issue/KT-30414) "Replace return with 'if' expression" drops return label
- [`KT-30426`](https://youtrack.jetbrains.com/issue/KT-30426) Don't preserve extra line when adding remaining branches for when
- [`KT-30433`](https://youtrack.jetbrains.com/issue/KT-30433) "Convert member to extension" doesn't update external Kotlin calls
- [`KT-30117`](https://youtrack.jetbrains.com/issue/KT-30117) Kotlin unused import analysis accesses file editor manager model outside UI thread
- [`KT-29143`](https://youtrack.jetbrains.com/issue/KT-29143) Unnecessary primary `constructor` keyword inspection
- [`KT-29444`](https://youtrack.jetbrains.com/issue/KT-29444) "Make public" intention does not remove additional white-space to conform to proper style
- [`KT-30337`](https://youtrack.jetbrains.com/issue/KT-30337) Do not propose to move variable declaration into "when" if it's not used inside the when-expression

### IDE. Multiplatform

- [`KT-29918`](https://youtrack.jetbrains.com/issue/KT-29918) Outdated Ktor version in Kotlin (JS Client/JVM Server) multiplatform project generated via New Project Wizard

### IDE. Navigation

- [`KT-26924`](https://youtrack.jetbrains.com/issue/KT-26924) Overriding Methods list has more values than it should be in case of inline class
- [`KT-28661`](https://youtrack.jetbrains.com/issue/KT-28661) "Is implemented in" gutter icon shows duplicate function implementations in inline classes
- [`KT-28838`](https://youtrack.jetbrains.com/issue/KT-28838) Group by file structure doesn't work for text search in Kotlin

### IDE. Refactorings

- [`KT-27602`](https://youtrack.jetbrains.com/issue/KT-27602) Kotlin property renaming change target name several times  during rename making it hard to process it by reference handlers
- [`KT-29062`](https://youtrack.jetbrains.com/issue/KT-29062) Extract Superclass refactoring throws Exception if sourceRoots.size() <= 1
- [`KT-29796`](https://youtrack.jetbrains.com/issue/KT-29796) Label rename refactoring does not work on label usage

### IDE. Scratch

- [`KT-23985`](https://youtrack.jetbrains.com/issue/KT-23985) Allow to run Kotlin Worksheet without module classpath
- [`KT-27955`](https://youtrack.jetbrains.com/issue/KT-27955) Interactive mode for Kotlin Scratch files
- [`KT-28958`](https://youtrack.jetbrains.com/issue/KT-28958) Exception "Read access is allowed from event dispatch thread or inside read-action only" when running a scratch file with "Use REPL" and "Make before Run" enabled
- [`KT-30200`](https://youtrack.jetbrains.com/issue/KT-30200) "java.lang.Throwable: Couldn't find expression with start line ..." on edition of a scratch file during its execution with interactive mode enabled

### IDE. Script

- [`KT-29770`](https://youtrack.jetbrains.com/issue/KT-29770) IntelliJ IDEA makes too many requests for the classpath of a Gradle Kotlin build script
- [`KT-29893`](https://youtrack.jetbrains.com/issue/KT-29893) IDE is frozen during project configuration because of `ScriptTemplatesFromDependenciesProvider`
- [`KT-30146`](https://youtrack.jetbrains.com/issue/KT-30146) Preferences from Kotlin scripting section reset to default after project reopening

### IDE. Tests Support

- [`KT-25956`](https://youtrack.jetbrains.com/issue/KT-25956) With failed test function class gutter icon is "failure", but function icon is "success"

### IDE. Wizards

- [`KT-17829`](https://youtrack.jetbrains.com/issue/KT-17829) Please unify naming of Kotlin projects and frameworks for JVM
- [`KT-28941`](https://youtrack.jetbrains.com/issue/KT-28941) Tip of the day: obsolete project types from "New project wizard"

### Libraries

- [`KT-27108`](https://youtrack.jetbrains.com/issue/KT-27108) `.toDouble()` and `.toFloat()` conversions for unsigned types
- [`KT-29520`](https://youtrack.jetbrains.com/issue/KT-29520) Random.Default cannot be used asJavaRandom
- [`KT-30109`](https://youtrack.jetbrains.com/issue/KT-30109) Documentation for Result.onSuccess and Result.onFailure are flipped around
- [`KT-26378`](https://youtrack.jetbrains.com/issue/KT-26378) 'contains' overloads for unsigned integer ranges with other unsigned integer types
- [`KT-26410`](https://youtrack.jetbrains.com/issue/KT-26410) High-order function overloads for unsigned arrays
- [`KT-27262`](https://youtrack.jetbrains.com/issue/KT-27262) Binary search for specialized arrays of unsigned integers
- [`KT-28339`](https://youtrack.jetbrains.com/issue/KT-28339) Add `fill` extension function for unsigned primitive arrays
- [`KT-28397`](https://youtrack.jetbrains.com/issue/KT-28397) UByteArray plus UByteArray = List<UByte>
- [`KT-28779`](https://youtrack.jetbrains.com/issue/KT-28779) Implement method sum() for arrays of unsigned primitives
- [`KT-29151`](https://youtrack.jetbrains.com/issue/KT-29151) Documentation for CharSequence.take() & String.take() shows examples of Iterable<T>.take()
- [`KT-30035`](https://youtrack.jetbrains.com/issue/KT-30035) add max/maxOf/min/minOf for unsigned types
- [`KT-30051`](https://youtrack.jetbrains.com/issue/KT-30051) elementAt extension function of Array/PrimitiveAray/UnsignedArray does not throw IndexOutOfBoundException on incorrect index (JS only)
- [`KT-30141`](https://youtrack.jetbrains.com/issue/KT-30141) JS: document Array.get behavior
- [`KT-30704`](https://youtrack.jetbrains.com/issue/KT-30704) Documentation of Random function not quite correct

### Tools. CLI

- [`KT-26240`](https://youtrack.jetbrains.com/issue/KT-26240) Support JVM bytecode targets 9, 10, 11, 12

### Tools. Gradle

- [`KT-12295`](https://youtrack.jetbrains.com/issue/KT-12295) Gradle IC: Compile error leads to non-incremental build
- [`KT-12700`](https://youtrack.jetbrains.com/issue/KT-12700) Add a way to diagnose IC problems
- [`KT-26275`](https://youtrack.jetbrains.com/issue/KT-26275) Check new MPP IC
- [`KT-27885`](https://youtrack.jetbrains.com/issue/KT-27885) Drop support for Gradle 3.x and earlier
- [`KT-27886`](https://youtrack.jetbrains.com/issue/KT-27886) Drop support for Android Gradle plugin 2.x
- [`KT-28552`](https://youtrack.jetbrains.com/issue/KT-28552) Gradle 4.7 import fails on Kotlin/mpp projects with Java11
- [`KT-29275`](https://youtrack.jetbrains.com/issue/KT-29275) Drop support for Gradle 4.0
- [`KT-29758`](https://youtrack.jetbrains.com/issue/KT-29758) Gradle build failed with exception on publication of a multiplatform library with Gradle metadata enabled: org.jetbrains.kotlin.gradle.plugin.mpp.HierarchyAttributeContainer cannot be cast to org.gradle.api.internal.attributes.AttributeContainerInternal
- [`KT-29966`](https://youtrack.jetbrains.com/issue/KT-29966) Fix inter-project IC with new MPP for JS/JVM targets
- [`KT-27059`](https://youtrack.jetbrains.com/issue/KT-27059) Ensure a dependency on the multiplatform project in the POM when publishing a single-platform module with the `maven` plugin
- [`KT-29971`](https://youtrack.jetbrains.com/issue/KT-29971) ConcurrentModificationException in Kotlin Gradle plugin (GradleCompilerRunner.buildModulesInfo)
- [`KT-21030`](https://youtrack.jetbrains.com/issue/KT-21030) Automatically detect java 1.8 sources in kotlin-android gradle plugin
- [`KT-27675`](https://youtrack.jetbrains.com/issue/KT-27675) Enable Kapt build cache by default
- [`KT-27714`](https://youtrack.jetbrains.com/issue/KT-27714) Kotlin MPP Android targets don't have their attributes copied to the configurations of the compilations
- [`KT-29761`](https://youtrack.jetbrains.com/issue/KT-29761) Inter-project IC does not work for kaptGenerateStubs* tasks on Android
- [`KT-29823`](https://youtrack.jetbrains.com/issue/KT-29823) Update 'org.gradle.usage' attribute rules to support the 'JAVA_API_JARS' value
- [`KT-29964`](https://youtrack.jetbrains.com/issue/KT-29964) A universal Gradle DSL way of configuring all compilations of all targets doesn't work for Android target of a multiplatform project
- [`KT-30276`](https://youtrack.jetbrains.com/issue/KT-30276) Warn if the Kotlin Gradle plugin is loaded multiple times
- [`KT-30322`](https://youtrack.jetbrains.com/issue/KT-30322) Memory leak in CompilationSourceSetUtil
- [`KT-30492`](https://youtrack.jetbrains.com/issue/KT-30492) Classes not removed for out/in process compilation

### Tools. J2K

- [`KT-29713`](https://youtrack.jetbrains.com/issue/KT-29713) java.lang.IllegalStateException at converting  @RestController java file to Kotlin file

### Tools. JPS

- [`KT-30137`](https://youtrack.jetbrains.com/issue/KT-30137) Deadlock during concurrent classloading

### Tools. Maven

- [`KT-29251`](https://youtrack.jetbrains.com/issue/KT-29251) NSME: MavenProjectsManager.scheduleArtifactsDownloading() at KotlinMavenImporter.scheduleDownloadStdlibSources()

### Tools. REPL

- [`KT-19276`](https://youtrack.jetbrains.com/issue/KT-19276) Console spam when opening idea-community project in debug IDEA

### Tools. Scripts

- [`KT-29296`](https://youtrack.jetbrains.com/issue/KT-29296) Script evaluation - impossible to set base classloader to null
- [`KT-27051`](https://youtrack.jetbrains.com/issue/KT-27051) Support dynamic versions in @file:DependsOn
- [`KT-27815`](https://youtrack.jetbrains.com/issue/KT-27815) Compiler options in the scripting compilation configuration are ignored on compilation/evaluation
- [`KT-28593`](https://youtrack.jetbrains.com/issue/KT-28593) Idea tries to associate file type with the script definition discovery file
- [`KT-29319`](https://youtrack.jetbrains.com/issue/KT-29319) scripts default jvmTarget causes inlining problems - default should be 1.8
- [`KT-29741`](https://youtrack.jetbrains.com/issue/KT-29741) KJvmCompiledScript can not be deserialized KJvmCompiledModule if it's null
- [`KT-30210`](https://youtrack.jetbrains.com/issue/KT-30210) Coroutines in main.kts crash with NoSuchMethodError because kotlin-main-kts.jar has embedded coroutines

### Tools. kapt

- [`KT-26977`](https://youtrack.jetbrains.com/issue/KT-26977) kapt plugin applied in platform.jvm module preventing visibility of common code
- [`KT-27506`](https://youtrack.jetbrains.com/issue/KT-27506) Kapt error "no interface expected here" in class implementing interface with secondary constructor
- [`KT-28220`](https://youtrack.jetbrains.com/issue/KT-28220) kapt can generate invalid stub files for imports of enum constants
- [`KT-28306`](https://youtrack.jetbrains.com/issue/KT-28306) Cannot extend an generic interface with function body while using kapt and correctErrorTypes in Kotlin 1.3
- [`KT-23880`](https://youtrack.jetbrains.com/issue/KT-23880) Kapt: Support incremental annotation processors
- [`KT-29302`](https://youtrack.jetbrains.com/issue/KT-29302) Java classes doesn't resolve Kotlin classes when kapt.use.worker.api = true
- [`KT-30163`](https://youtrack.jetbrains.com/issue/KT-30163) Kapt: Javadoc in Java source model mangled (leading asterisks are preserved)

### Docs & Examples

- [`KT-30091`](https://youtrack.jetbrains.com/issue/KT-30091) KClass documentation incorrectly shows all members available on all platforms
- [`KT-30100`](https://youtrack.jetbrains.com/issue/KT-30100) Clarify Map.toSortedMap docs
- [`KT-30418`](https://youtrack.jetbrains.com/issue/KT-30418) Documentation for floor() and ceil() functions is misleading
- [`KT-29373`](https://youtrack.jetbrains.com/issue/KT-29373) MutableSet.add documentation is confusing

## 1.3.21

### Compiler

#### Fixes

- [`KT-29475`](https://youtrack.jetbrains.com/issue/KT-29475) IllegalArgumentException at getAbstractTypeFromDescriptor with deeply nested expression inside function named with a right parenthesis
- [`KT-29479`](https://youtrack.jetbrains.com/issue/KT-29479) WARN: Could not read file on Java classes from JDK 11+
- [`KT-29360`](https://youtrack.jetbrains.com/issue/KT-29360) Kotlin 1.3.20-eap-100: This marker function should never been called. Looks like compiler did not eliminate it properly. Please, report an issue if you caught this exception.

### IDE

#### Fixes

- [`KT-29486`](https://youtrack.jetbrains.com/issue/KT-29486) Throwable: "Could not find correct module information" through IdeaKotlinUastResolveProviderService.getBindingContext() and ReplaceWithAnnotationAnalyzer.analyzeOriginal()
- [`KT-29394`](https://youtrack.jetbrains.com/issue/KT-29394) Kotlin 1.3.20 EAP: Excess log messages with `kotlin.parallel.tasks.in.project=true`
- [`KT-29474`](https://youtrack.jetbrains.com/issue/KT-29474) Regression in 1.3.20: Kotlin IDE plugin parses all *.gradle.kts files when any class in buildSrc is opened
- [`KT-29290`](https://youtrack.jetbrains.com/issue/KT-29290) Warning "function returning deferred with a name that does not end with async" should not be displayed for let/also/apply...
- [`KT-29494`](https://youtrack.jetbrains.com/issue/KT-29494) Don't report BooleanLiteralArgumentInspection in batch (offline) mode with INFORMATION severity
- [`KT-29525`](https://youtrack.jetbrains.com/issue/KT-29525) turning on parallel tasks causes java.lang.NoClassDefFoundError: Could not initialize class kotlin.Unit sometimes
- [`KT-27769`](https://youtrack.jetbrains.com/issue/KT-27769) Change the DSL marker icon
- [`KT-29118`](https://youtrack.jetbrains.com/issue/KT-29118) Log polluted with multiple "Kotlin does not support alternative resolve" reports

### IDE. Multiplatform

- [`KT-28128`](https://youtrack.jetbrains.com/issue/KT-28128) MPP Kotlin/Native re-downloads POM files on IDE Gradle refresh

### IDE. REPL

- [`KT-29400`](https://youtrack.jetbrains.com/issue/KT-29400) IDE REPL in Gradle project: "IllegalStateException: consoleView must not be null" on module build

### Libraries

#### Fixes

- [`KT-29612`](https://youtrack.jetbrains.com/issue/KT-29612) jlink refuses to consume stdlib containing non-public package kotlin.native

### Tools. CLI

- [`KT-29596`](https://youtrack.jetbrains.com/issue/KT-29596) "AssertionError: Cannot load extensions/common.xml from kotlin-compiler.jar" on IBM JDK 8

### Tools. Gradle

- [`KT-29476`](https://youtrack.jetbrains.com/issue/KT-29476) 1.3.20 MPP Android publishing common api configuration with runtime scope
- [`KT-29725`](https://youtrack.jetbrains.com/issue/KT-29725) MPP Gradle 5.2: NoSuchMethodError in WrapUtil
- [`KT-29485`](https://youtrack.jetbrains.com/issue/KT-29485) In MPP with Gradle module metadata, POM rewriting does not replace the root module publication with a platform one if the former has a custom artifact ID

### Tools. Scripts

- [`KT-29490`](https://youtrack.jetbrains.com/issue/KT-29490) Regression in 1.3.20: Kotlin Jsr223 script engine cannot handle functional return types

### Tools. Kapt

- [`KT-29481`](https://youtrack.jetbrains.com/issue/KT-29481) Annotation processors run on androidTest source set even without the kaptAndroidTest declaration
- [`KT-29513`](https://youtrack.jetbrains.com/issue/KT-29513) kapt throws "ZipException: zip END header not found", when Graal SVM jar in classpath

## 1.3.20

### Android

- [`KT-22571`](https://youtrack.jetbrains.com/issue/KT-22571) Android: Configure Kotlin should add implementation dependency instead of compile

### Compiler

#### New Features

- [`KT-14416`](https://youtrack.jetbrains.com/issue/KT-14416) Support of @PolymorphicSignature in Kotlin compiler
- [`KT-22704`](https://youtrack.jetbrains.com/issue/KT-22704) Allow expect annotations with actual typealias to Java to have default argument values both in expected and in actual
- [`KT-26165`](https://youtrack.jetbrains.com/issue/KT-26165) Support VarHandle in JVM codegen
- [`KT-26999`](https://youtrack.jetbrains.com/issue/KT-26999) Inspection for unused main parameter in Kotlin 1.3

#### Performance Improvements

- [`KT-16867`](https://youtrack.jetbrains.com/issue/KT-16867) Proguard can't unbox Kotlin enums to integers
- [`KT-23466`](https://youtrack.jetbrains.com/issue/KT-23466) kotlin compiler opens-reads-closes .class files many times over
- [`KT-25613`](https://youtrack.jetbrains.com/issue/KT-25613) Optimise boxing of inline class values inside string templates

#### Fixes

- [`KT-2680`](https://youtrack.jetbrains.com/issue/KT-2680) JVM backend should generate synthetic constructors for enum entries (as javac does).
- [`KT-6574`](https://youtrack.jetbrains.com/issue/KT-6574) Enum entry classes should be compiled to package private classes
- [`KT-8341`](https://youtrack.jetbrains.com/issue/KT-8341) Local variable cannot have type parameters
- [`KT-14529`](https://youtrack.jetbrains.com/issue/KT-14529) JS: annotations on property accessors are not serialized
- [`KT-15453`](https://youtrack.jetbrains.com/issue/KT-15453) Annotations are ignored on accessors of private properties
- [`KT-18053`](https://youtrack.jetbrains.com/issue/KT-18053) Unexpected behavior with "in" infix operator and ConcurrentHashMap
- [`KT-18592`](https://youtrack.jetbrains.com/issue/KT-18592) Compiler cannot resolve trait-based superclass of Groovy dependency
- [`KT-19613`](https://youtrack.jetbrains.com/issue/KT-19613) "Public property exposes its private type" not reported for primary constructor properties
- [`KT-20344`](https://youtrack.jetbrains.com/issue/KT-20344) Unused private setter created for property
- [`KT-21862`](https://youtrack.jetbrains.com/issue/KT-21862) java.lang.NoSuchFieldError when calling isInitialized on a lateinit "field" of a companion object
- [`KT-21946`](https://youtrack.jetbrains.com/issue/KT-21946) Compilation error during default lambda inlining when it returns anonymous object
- [`KT-22154`](https://youtrack.jetbrains.com/issue/KT-22154) Warning: Stripped invalid locals information from 1 method when compiling with D8
- [`KT-23369`](https://youtrack.jetbrains.com/issue/KT-23369) Internal compiler error in SMAPParser.parse
- [`KT-23543`](https://youtrack.jetbrains.com/issue/KT-23543) Back-end (JVM) Internal error: Couldn't inline method
- [`KT-23739`](https://youtrack.jetbrains.com/issue/KT-23739) CompilationException: Back-end (JVM) Internal error: Couldn't inline method call: Unmapped line number in inlined function
- [`KT-24156`](https://youtrack.jetbrains.com/issue/KT-24156) For-loop optimization should not be applied in case of custom iterator
- [`KT-24672`](https://youtrack.jetbrains.com/issue/KT-24672) JVM BE: Wrong range is generated in LVT for variables with "late" assignment
- [`KT-24780`](https://youtrack.jetbrains.com/issue/KT-24780) Recursive suspend local functions: "Expected an object reference, but found ."
- [`KT-24937`](https://youtrack.jetbrains.com/issue/KT-24937) Exception from parser (EA-76217)
- [`KT-25058`](https://youtrack.jetbrains.com/issue/KT-25058) Fix deprecated API usage in RemappingClassBuilder
- [`KT-25288`](https://youtrack.jetbrains.com/issue/KT-25288) SOE when inline class is recursive through type parameter upper bound
- [`KT-25295`](https://youtrack.jetbrains.com/issue/KT-25295) “Couldn't transform method node” error on compiling inline class with inherited interface method call
- [`KT-25424`](https://youtrack.jetbrains.com/issue/KT-25424) No coercion to Unit when type argument specified explicitly
- [`KT-25702`](https://youtrack.jetbrains.com/issue/KT-25702) @JvmOverloads should not be allowed on constructors of annotation classes
- [`KT-25893`](https://youtrack.jetbrains.com/issue/KT-25893) crossinline suspend function leads to IllegalStateException: call to 'resume' before 'invoke' with coroutine or compile error
- [`KT-25907`](https://youtrack.jetbrains.com/issue/KT-25907) "Backend Internal error" for a nullable loop variable with explicitly declared type in a for-loop over String
- [`KT-25922`](https://youtrack.jetbrains.com/issue/KT-25922) Back-end Internal error : Couldn't inline method : Lambda inlining : invoke(Continuation) : Trying to access skipped parameter
- [`KT-26126`](https://youtrack.jetbrains.com/issue/KT-26126) Front-end doesn't check that fun with contract and `callsInPlace` effect is an inline function; compiler crashes on val initialization
- [`KT-26366`](https://youtrack.jetbrains.com/issue/KT-26366) UseExperimental with full qualified reference to marker annotation class is reported as error
- [`KT-26384`](https://youtrack.jetbrains.com/issue/KT-26384) Compiler crash with nested multi-catch try, finally block and inline function
- [`KT-26505`](https://youtrack.jetbrains.com/issue/KT-26505) Improve error message on missing script base class kotlin.script.templates.standard.ScriptTemplateWithArgs
- [`KT-26506`](https://youtrack.jetbrains.com/issue/KT-26506) Incorrect bytecode generated for inner class inside inline class referencing outer 'this'
- [`KT-26508`](https://youtrack.jetbrains.com/issue/KT-26508) Incorrect accessor generated for private inline class method call from lambda
- [`KT-26509`](https://youtrack.jetbrains.com/issue/KT-26509) Internal compiler error on generating inline class private method call from companion object
- [`KT-26554`](https://youtrack.jetbrains.com/issue/KT-26554) VerifyError: Bad type on operand stack for inline class with default parameter of underlying type
- [`KT-26582`](https://youtrack.jetbrains.com/issue/KT-26582) Array literal of a primitive wrapper class is loaded as a primitive array literal
- [`KT-26608`](https://youtrack.jetbrains.com/issue/KT-26608) Couldn't inline method call. RuntimeException: Trying to access skipped parameter: Ljava/lang/Object;
- [`KT-26658`](https://youtrack.jetbrains.com/issue/KT-26658) Trying to access skipped parameter exception in code with crossinline suspend lambda with suspend function with default parameter call
- [`KT-26715`](https://youtrack.jetbrains.com/issue/KT-26715) NullPointerException for an inline class constructor reference
- [`KT-26848`](https://youtrack.jetbrains.com/issue/KT-26848) Incorrect line in coroutine debug metadata for first suspension point
- [`KT-26908`](https://youtrack.jetbrains.com/issue/KT-26908) Inline classes can't have a parameter with a default value (Platform declaration clash)
- [`KT-26931`](https://youtrack.jetbrains.com/issue/KT-26931) NSME “InlineClass.foo-impl(LIFace;)I” on calling inherited method from inline class instance
- [`KT-26932`](https://youtrack.jetbrains.com/issue/KT-26932) CCE “Foo cannot be cast to java.lang.String” when accessing underlying value of inline class through reflection
- [`KT-26998`](https://youtrack.jetbrains.com/issue/KT-26998) Default extension fun call in generic Kotlin interface with inline class substituted type of extension receiver fails with internal compiler error
- [`KT-27025`](https://youtrack.jetbrains.com/issue/KT-27025) Inline class access to private companion object fun fails with VerifyError
- [`KT-27070`](https://youtrack.jetbrains.com/issue/KT-27070) Delegated property with inline class type delegate fails with internal error in codegen
- [`KT-27078`](https://youtrack.jetbrains.com/issue/KT-27078) Inline class instance captured in closure fails with internal error (incorrect bytecode generated)
- [`KT-27107`](https://youtrack.jetbrains.com/issue/KT-27107) JvmStatic in inline class companion doesn't generate static method in the class
- [`KT-27113`](https://youtrack.jetbrains.com/issue/KT-27113) Inline class's `toString` is not called when it is used in string extrapolation
- [`KT-27140`](https://youtrack.jetbrains.com/issue/KT-27140) Couldn't inline method call 'ByteArray' with inline class
- [`KT-27162`](https://youtrack.jetbrains.com/issue/KT-27162) Incorrect container is generated to callable reference classes for references to inline class members
- [`KT-27259`](https://youtrack.jetbrains.com/issue/KT-27259) "Internal error: wrong code generated" for nullable inline class with an inline class underlying type
- [`KT-27318`](https://youtrack.jetbrains.com/issue/KT-27318) Interface implementation by delegation to inline class type delegate fails with internal error in codegen
- [`KT-27358`](https://youtrack.jetbrains.com/issue/KT-27358) Boxed inline class type default parameter values fail with CCE at run-time
- [`KT-27416`](https://youtrack.jetbrains.com/issue/KT-27416) "IllegalStateException: Backend Internal error" for inline class with a function with default argument value
- [`KT-27429`](https://youtrack.jetbrains.com/issue/KT-27429) "-java-parameters" compiler argument fails in constructor when there is an inline class parameter present
- [`KT-27513`](https://youtrack.jetbrains.com/issue/KT-27513) Backend Internal Error when using inline method inside inline class
- [`KT-27560`](https://youtrack.jetbrains.com/issue/KT-27560) Executing getter of property with type kotlin.reflect.KSuspendFunction1 throws MalformedParameterizedTypeException
- [`KT-27705`](https://youtrack.jetbrains.com/issue/KT-27705) Internal compiler error (incorrect bytecode generated) when inner class constructor inside inline class references inline class primary val
- [`KT-27706`](https://youtrack.jetbrains.com/issue/KT-27706) Internal compiler error (incorrect bytecode generated) when inner class inside inline class accepts inline class parameter
- [`KT-27732`](https://youtrack.jetbrains.com/issue/KT-27732) Using type inference on platform types corresponding to unsigned types causes compiler error
- [`KT-27737`](https://youtrack.jetbrains.com/issue/KT-27737) CCE for delegated property of inline class type
- [`KT-27762`](https://youtrack.jetbrains.com/issue/KT-27762) The lexer crashes when a vertical tabulation is used
- [`KT-27774`](https://youtrack.jetbrains.com/issue/KT-27774) Update asm to 7.0 in Kotlin backend
- [`KT-27948`](https://youtrack.jetbrains.com/issue/KT-27948) "Argument 2: expected R, but found I" for `equals` operator on nullable and non-null unsigned types
- [`KT-28054`](https://youtrack.jetbrains.com/issue/KT-28054) Inline class: "Cannot pop operand off an empty stack" for calling private secondary constructor from companion object
- [`KT-28061`](https://youtrack.jetbrains.com/issue/KT-28061) Safe call operator and contracts: false negative "A 'return' expression required in a function with a block body"
- [`KT-28185`](https://youtrack.jetbrains.com/issue/KT-28185) Incorrect behaviour of javaClass intrinsic for receivers of inline class type
- [`KT-28188`](https://youtrack.jetbrains.com/issue/KT-28188) CCE when bound callable reference with receiver of inline class type is passed to inline function
- [`KT-28237`](https://youtrack.jetbrains.com/issue/KT-28237) CoroutineStackFrame uses slashes instead of dots in FQN
- [`KT-28361`](https://youtrack.jetbrains.com/issue/KT-28361) Class literal for inline class should return KClass object of the wrapper
- [`KT-28385`](https://youtrack.jetbrains.com/issue/KT-28385) Rewrite at slice FUNCTION in MPP on "red" code
- [`KT-28405`](https://youtrack.jetbrains.com/issue/KT-28405) VE “Bad type on operand stack” at runtime on creating inline class with UIntArray inside
- [`KT-28585`](https://youtrack.jetbrains.com/issue/KT-28585) Inline classes not properly boxed when accessing a `var` (from enclosing scope) from lambda
- [`KT-28847`](https://youtrack.jetbrains.com/issue/KT-28847) Compilation fails with "AssertionError: Rewrite at slice FUNCTOR" on compiling complicated case with delegating property
- [`KT-28879`](https://youtrack.jetbrains.com/issue/KT-28879) "AnalyzerException: Expected I, but found R" when compiling javaClass on inline class value
- [`KT-28920`](https://youtrack.jetbrains.com/issue/KT-28920) "AnalyzerException: Expected I, but found R" when compiling javaObjectType/javaPrimitiveType with inline classes
- [`KT-28965`](https://youtrack.jetbrains.com/issue/KT-28965) Unsound smartcast to definitely not-null if value of one generic type is cast to other generic type
- [`KT-28983`](https://youtrack.jetbrains.com/issue/KT-28983) Wrong mapping of flexible inline class type to primitive type

### IDE

#### New Features

- [`KT-25906`](https://youtrack.jetbrains.com/issue/KT-25906) Kotlin language injection doesn't evaluate constants in string templates
- [`KT-27461`](https://youtrack.jetbrains.com/issue/KT-27461) Provide live template to generate `main()` with no parameters
- [`KT-28371`](https://youtrack.jetbrains.com/issue/KT-28371) Automatically align ?: (elvis operator) after call on the new line

#### Performance Improvements

- [`KT-23738`](https://youtrack.jetbrains.com/issue/KT-23738) Provide stubs for annotation value argument list 
- [`KT-25410`](https://youtrack.jetbrains.com/issue/KT-25410) Opening Settings freezes the UI for 23 seconds
- [`KT-27832`](https://youtrack.jetbrains.com/issue/KT-27832) Improve performance of KotlinGradleProjectResolverExtension
- [`KT-28755`](https://youtrack.jetbrains.com/issue/KT-28755) Optimize searching constructor delegation calls
- [`KT-29297`](https://youtrack.jetbrains.com/issue/KT-29297) Improve performance of light classes in IDE (Java-to-Kotlin interop)

#### Fixes

- [`KT-9840`](https://youtrack.jetbrains.com/issue/KT-9840) Right parenthesis doesn't appear after class name before the colon
- [`KT-13420`](https://youtrack.jetbrains.com/issue/KT-13420) Extend Selection: lambda: whole literal with braces is selected after parameters
- [`KT-17502`](https://youtrack.jetbrains.com/issue/KT-17502) Do not disable "Generate equals and hashCode" actions for data classes
- [`KT-22590`](https://youtrack.jetbrains.com/issue/KT-22590) Create Kotlin SDK if it's absent on importing from gradle/maven Kotlin (JavaScript) projects and on configuring java project to Kotlin(JavaScript), Kotlin(Common)
- [`KT-23268`](https://youtrack.jetbrains.com/issue/KT-23268) IntelliJ plugin: Variables from destructing declarations are not syntax colored as variables
- [`KT-23864`](https://youtrack.jetbrains.com/issue/KT-23864) Copyright message is duplicated in kotlin file in root package after updating copyright
- [`KT-25156`](https://youtrack.jetbrains.com/issue/KT-25156) SOE in IDE on destructuring delegated property declaration
- [`KT-25681`](https://youtrack.jetbrains.com/issue/KT-25681) Remove "Coroutines (experimental)" settings from IDE and do not pass `-Xcoroutines` to JPS compiler (since 1.3)
- [`KT-26868`](https://youtrack.jetbrains.com/issue/KT-26868) MPP: Gradle import: test dependencies get Compile scope
- [`KT-26987`](https://youtrack.jetbrains.com/issue/KT-26987) "Extend Selection" is missing for labeled return
- [`KT-27095`](https://youtrack.jetbrains.com/issue/KT-27095) Kotlin configuration: update EAP repositories to use https instead of http
- [`KT-27321`](https://youtrack.jetbrains.com/issue/KT-27321) Cannot init component state if "internalArguments" presents in xml project structure (kotlinc.xml)
- [`KT-27375`](https://youtrack.jetbrains.com/issue/KT-27375) Kotlin Gradle DSL script: "Unable to get Gradle home directory" in new project with new Gradle wrapper
- [`KT-27380`](https://youtrack.jetbrains.com/issue/KT-27380) `KotlinStringLiteralTextEscaper` returns wrong offset on unparseable elements
- [`KT-27491`](https://youtrack.jetbrains.com/issue/KT-27491) MPP JVM/JS wizard: Use Ktor in the skeleton
- [`KT-27492`](https://youtrack.jetbrains.com/issue/KT-27492) Create some MPP wizard tests
- [`KT-27530`](https://youtrack.jetbrains.com/issue/KT-27530) Kotlin Gradle plugin overwrites the JDK set by jdkName property of the Gradle Idea plugin
- [`KT-27663`](https://youtrack.jetbrains.com/issue/KT-27663) Uast: don't store resolved descriptors in UElements
- [`KT-27907`](https://youtrack.jetbrains.com/issue/KT-27907) Exception on processing auto-generated classes from AS
- [`KT-27954`](https://youtrack.jetbrains.com/issue/KT-27954) Generate -> toString() using "Multiple templates with concatenation" should add spaces after commas
- [`KT-27941`](https://youtrack.jetbrains.com/issue/KT-27941) MPP: Gradle import with "using qualified names" creates 2 modules with the same content root
- [`KT-28199`](https://youtrack.jetbrains.com/issue/KT-28199) Could not get javaResolutionFacade for groovy elements
- [`KT-28348`](https://youtrack.jetbrains.com/issue/KT-28348) Don't log or wrap ProcessCanceledException
- [`KT-28401`](https://youtrack.jetbrains.com/issue/KT-28401) Show parameter info for lambdas during completion
- [`KT-28402`](https://youtrack.jetbrains.com/issue/KT-28402) Automatically indent || and && operators
- [`KT-28458`](https://youtrack.jetbrains.com/issue/KT-28458) New Project Wizard: move multiplatform projects to the new DSL
- [`KT-28513`](https://youtrack.jetbrains.com/issue/KT-28513) Bad Kotlin configuration when old syntax is used for configured Gradle project with >= 4.4 version
- [`KT-28556`](https://youtrack.jetbrains.com/issue/KT-28556) Wrong nullability for @JvmOverloads-generated method parameter in light classes
- [`KT-28997`](https://youtrack.jetbrains.com/issue/KT-28997) Couldn't get delegate for class from any local class or object in script
- [`KT-29027`](https://youtrack.jetbrains.com/issue/KT-29027) Kotlin LightAnnotations don't handle vararg class literals

### IDE. Android

- [`KT-23560`](https://youtrack.jetbrains.com/issue/KT-23560) Scratch: impossible to run scratch file from Android Studio
- [`KT-25450`](https://youtrack.jetbrains.com/issue/KT-25450) NoClassDefFoundError when trying to run a scratch file in Android Studio 3.1.3, Kotlin 1.2.51
- [`KT-26764`](https://youtrack.jetbrains.com/issue/KT-26764) `kotlin` content root isn't generated for Android module of a multiplatform project on Gradle import

### IDE. Code Style, Formatting

- [`KT-5590`](https://youtrack.jetbrains.com/issue/KT-5590) kotlin: line comment must not be on first column by default
- [`KT-24496`](https://youtrack.jetbrains.com/issue/KT-24496) IntelliJ IDEA: Formatting around addition / subtraction not correct for Kotlin
- [`KT-25417`](https://youtrack.jetbrains.com/issue/KT-25417) Incorrect formatting for comments on property accessors
- [`KT-27847`](https://youtrack.jetbrains.com/issue/KT-27847) Destructured declaration continued on the next line is formatted with double indent
- [`KT-28070`](https://youtrack.jetbrains.com/issue/KT-28070) Code style: "Align when multiline" option for "extends / implements list" changes formating of enum constants constructor parameters
- [`KT-28227`](https://youtrack.jetbrains.com/issue/KT-28227) Formatter should not allow enum entries to be on one line with opening brace
- [`KT-28484`](https://youtrack.jetbrains.com/issue/KT-28484) Bad formatting for assignment when continuation for assignments is disabled

### IDE. Completion

- [`KT-18089`](https://youtrack.jetbrains.com/issue/KT-18089) Completion for nullable types without safe call rendered in gray color is barely visible
- [`KT-20706`](https://youtrack.jetbrains.com/issue/KT-20706) KDoc: Unneeded completion is invoked after typing a number/digit in a kdoc comment
- [`KT-22579`](https://youtrack.jetbrains.com/issue/KT-22579) Smart completion should present enum constants with higher rank
- [`KT-23834`](https://youtrack.jetbrains.com/issue/KT-23834) Code completion and auto import do not suggest extension that differs from member only in type parameter
- [`KT-25312`](https://youtrack.jetbrains.com/issue/KT-25312) Autocomplete for overridden members in `expected` class inserts extra `override` word
- [`KT-26632`](https://youtrack.jetbrains.com/issue/KT-26632) Completion: "data class" instead of "data"
- [`KT-27916`](https://youtrack.jetbrains.com/issue/KT-27916) Autocomplete val when auto-completing const

### IDE. Debugger

#### Fixes

- [`KT-13268`](https://youtrack.jetbrains.com/issue/KT-13268) Can't quick evaluate expression with Alt + Click without get operator
- [`KT-14075`](https://youtrack.jetbrains.com/issue/KT-14075) Debugger: Property syntax accesses private Java field rather than synthetic property accessor
- [`KT-22366`](https://youtrack.jetbrains.com/issue/KT-22366) Debugger doesn't stop on certain expressions
- [`KT-23585`](https://youtrack.jetbrains.com/issue/KT-23585) Evaluation of a static interface method call fails
- [`KT-24343`](https://youtrack.jetbrains.com/issue/KT-24343) Debugger, Step Over: IllegalStateException on two consecutive breakpoints when first breakpoint is on an inline function call
- [`KT-24959`](https://youtrack.jetbrains.com/issue/KT-24959) Evaluating my breakpoint condition fails with exception
- [`KT-25667`](https://youtrack.jetbrains.com/issue/KT-25667) Exception in logs from WeakBytecodeDebugInfoStorage (NoStrataPositionManagerHelper)
- [`KT-26795`](https://youtrack.jetbrains.com/issue/KT-26795) Debugger crashes with NullPointerException when evaluating const value in companion object
- [`KT-26798`](https://youtrack.jetbrains.com/issue/KT-26798) Check that step into works with overrides in inline classes
- [`KT-27414`](https://youtrack.jetbrains.com/issue/KT-27414) Use "toString" to render values of inline classes in debugger
- [`KT-27462`](https://youtrack.jetbrains.com/issue/KT-27462) Main without parameters just with inline fun call: Debug: last Step Over can't finish the process
- [`KT-28028`](https://youtrack.jetbrains.com/issue/KT-28028) IDEA is unable to find sources during debugging
- [`KT-28342`](https://youtrack.jetbrains.com/issue/KT-28342) Can't evaluate the synthetic 'field' variable
- [`KT-28487`](https://youtrack.jetbrains.com/issue/KT-28487) ISE “resultValue is null: cannot find method generated_for_debugger_fun” on evaluating value of inline class

### IDE. Decompiler

- [`KT-27284`](https://youtrack.jetbrains.com/issue/KT-27284) Disable highlighting in decompiled Kotlin bytecode
- [`KT-27460`](https://youtrack.jetbrains.com/issue/KT-27460) "Show Kotlin bytecode": "Internal error: null" for an inline extension property from a different file

### IDE. Gradle

- [`KT-27265`](https://youtrack.jetbrains.com/issue/KT-27265) Unresolved reference in IDE on calling JVM source set members of a multiplatform project with Android target from a plain Kotlin/JVM module

### IDE. Gradle. Script

- [`KT-14862`](https://youtrack.jetbrains.com/issue/KT-14862) IDEA links to class file instead of source in buildSrc (Gradle/Kotlin script)
- [`KT-17231`](https://youtrack.jetbrains.com/issue/KT-17231) "Optimize Import" action not working for Gradle script kotlin.
- [`KT-21981`](https://youtrack.jetbrains.com/issue/KT-21981) Optimize imports on the fly does not take implicit imports into account in .kts files
- [`KT-24623`](https://youtrack.jetbrains.com/issue/KT-24623) Class defined in gradle buildSrc folder is marked as unused when it is actually used in Gradle Script Kotlin file
- [`KT-24705`](https://youtrack.jetbrains.com/issue/KT-24705) Script reports are shown in the editor only after caret move
- [`KT-24706`](https://youtrack.jetbrains.com/issue/KT-24706) Do not attach script reports if 'reload dependencies' isn't pressed
- [`KT-25354`](https://youtrack.jetbrains.com/issue/KT-25354) Gradle Kotlin-DSL: Changes of buildSrc are not visible from other modules
- [`KT-25619`](https://youtrack.jetbrains.com/issue/KT-25619) Intentions not working in buildSrc (Gradle)
- [`KT-27674`](https://youtrack.jetbrains.com/issue/KT-27674) Highlighting is skipped in files from buildSrc folder of Gradle project

### IDE. Hints

- [`KT-13118`](https://youtrack.jetbrains.com/issue/KT-13118) Parameter info is not shown for Kotlin last-argument lambdas
- [`KT-25162`](https://youtrack.jetbrains.com/issue/KT-25162) Parameter info for builder functions and lambdas
- [`KT-26689`](https://youtrack.jetbrains.com/issue/KT-26689) Lambda return expression hint not shown when returning a lambda from inside a lambda
- [`KT-27802`](https://youtrack.jetbrains.com/issue/KT-27802) The hint for the if-expression is duplicated inside each branch

### IDE. Inspections and Intentions

#### New Features

- [`KT-2029`](https://youtrack.jetbrains.com/issue/KT-2029) Add inspection for boolean literals passed without using named parameters feature
- [`KT-5071`](https://youtrack.jetbrains.com/issue/KT-5071) Properly surround a function invocation in string template by curly braces
- [`KT-5187`](https://youtrack.jetbrains.com/issue/KT-5187) Quick Fix to remove inline keyword on warning about performance benefits
- [`KT-6025`](https://youtrack.jetbrains.com/issue/KT-6025) Auto-remove toString() call in "Convert concatenation to template"
- [`KT-9983`](https://youtrack.jetbrains.com/issue/KT-9983) "'inline'' modifier is not allowed on virtual members." should have quickfix
- [`KT-12743`](https://youtrack.jetbrains.com/issue/KT-12743) Add Intention to convert nullable var to non-nullable lateinit
- [`KT-15525`](https://youtrack.jetbrains.com/issue/KT-15525) Inspection to warn on thread-blocking invocations from coroutines
- [`KT-17004`](https://youtrack.jetbrains.com/issue/KT-17004) There is no suggestion to add property to supertype
- [`KT-19668`](https://youtrack.jetbrains.com/issue/KT-19668) Inspection "Redundant else in if"
- [`KT-20273`](https://youtrack.jetbrains.com/issue/KT-20273) Inspection to report a setter of a property with a backing field that doesn't update the backing field
- [`KT-20626`](https://youtrack.jetbrains.com/issue/KT-20626) Inspection for '+= creates a new list under the hood'
- [`KT-23691`](https://youtrack.jetbrains.com/issue/KT-23691) Warn about `var` properties with default setter and getter that doesn't reference backing field
- [`KT-24515`](https://youtrack.jetbrains.com/issue/KT-24515) Intention to add an exception under the cursor to @Throws annotations
- [`KT-25171`](https://youtrack.jetbrains.com/issue/KT-25171) Inspection: Change indexed access operator on maps to `Map.getValue`
- [`KT-25620`](https://youtrack.jetbrains.com/issue/KT-25620) Inspection for functions returning Deferred
- [`KT-25718`](https://youtrack.jetbrains.com/issue/KT-25718) Add intention to convert SAM lambda to anonymous object
- [`KT-26236`](https://youtrack.jetbrains.com/issue/KT-26236) QuickFix for ASSIGN_OPERATOR_AMBIGUITY on mutable collection '+=', '-='
- [`KT-26511`](https://youtrack.jetbrains.com/issue/KT-26511) Inspection (without highlighting by default) for unlabeled return inside lambda
- [`KT-26653`](https://youtrack.jetbrains.com/issue/KT-26653) Intention to replace if-else with `x?.let { ... } ?: ...`
- [`KT-26724`](https://youtrack.jetbrains.com/issue/KT-26724) Inspection with a warning for implementation by delegation to a `var` property
- [`KT-26836`](https://youtrack.jetbrains.com/issue/KT-26836) Add quick fix for type mismatch between signed and unsigned types for constant literals
- [`KT-27007`](https://youtrack.jetbrains.com/issue/KT-27007) Intention: add label to return if scope is visually ambiguous
- [`KT-27075`](https://youtrack.jetbrains.com/issue/KT-27075) Add a quick fix/intention to create `expect` member for an added `actual` declaration
- [`KT-27445`](https://youtrack.jetbrains.com/issue/KT-27445) Add quickfix for compiler warning "DEPRECATED_JAVA_ANNOTATION"
- [`KT-28118`](https://youtrack.jetbrains.com/issue/KT-28118) Remove empty parentheses for annotation entries
- [`KT-28631`](https://youtrack.jetbrains.com/issue/KT-28631) Suggest to remove single lambda argument if its name is equal to `it`
- [`KT-28696`](https://youtrack.jetbrains.com/issue/KT-28696) Inspection: detect potentially ambiguous usage of coroutineContext
- [`KT-28699`](https://youtrack.jetbrains.com/issue/KT-28699) Add "Convert to also" intention

#### Performance Improvements

- [`KT-26969`](https://youtrack.jetbrains.com/issue/KT-26969) ConvertCallChainIntoSequence quick fix doesn't use sequences all the way

#### Fixes

- [`KT-4645`](https://youtrack.jetbrains.com/issue/KT-4645) Unexpected behevior of "Replace 'if' with 'when'" intention when called on second or third 'if'
- [`KT-5088`](https://youtrack.jetbrains.com/issue/KT-5088) "Add else branch" quickfix on when should not add braces
- [`KT-7555`](https://youtrack.jetbrains.com/issue/KT-7555) Omit braces when converting 'this' in 'Convert concatenation to template'
- [`KT-8820`](https://youtrack.jetbrains.com/issue/KT-8820) No "Change type" quick fix inside when
- [`KT-8875`](https://youtrack.jetbrains.com/issue/KT-8875) "Remove explicit type" produce red code for extension lambda
- [`KT-12479`](https://youtrack.jetbrains.com/issue/KT-12479) IDEA doesn't propose to replace all usages of deprecated annotation when it declared w/o parentheses
- [`KT-13311`](https://youtrack.jetbrains.com/issue/KT-13311) IDE marks fun finalize() as unused and says that its effective visibility is private
- [`KT-14555`](https://youtrack.jetbrains.com/issue/KT-14555) Strange 'iterate over Nothing' intention
- [`KT-15550`](https://youtrack.jetbrains.com/issue/KT-15550) Intention "Add names to call arguments" isn't available if one argument is a generic function call
- [`KT-15835`](https://youtrack.jetbrains.com/issue/KT-15835) "Leaking 'this' in constructor" for enum class
- [`KT-16338`](https://youtrack.jetbrains.com/issue/KT-16338) "Leaking 'this' in constructor" of non-final class when using 'this::class.java'
- [`KT-20040`](https://youtrack.jetbrains.com/issue/KT-20040) Kotlin Gradle script: unused import doesn't become grey
- [`KT-20725`](https://youtrack.jetbrains.com/issue/KT-20725) Cannot persist excluded methods for inspection "Accessor call that can be replaced with property syntax"
- [`KT-21520`](https://youtrack.jetbrains.com/issue/KT-21520) "Assignment should be lifted out of 'if'" false positive for arguments of different types
- [`KT-23134`](https://youtrack.jetbrains.com/issue/KT-23134) "Remove single lambda parameter" quick fix applied to a lambda parameter with explicit type breaks ::invoke reference on lambda
- [`KT-23512`](https://youtrack.jetbrains.com/issue/KT-23512) "Remove redundant receiver" quick fix makes generic function call incompilable when type could be inferred from removed receiver only
- [`KT-23639`](https://youtrack.jetbrains.com/issue/KT-23639) False positive "Unused symbol" for sealed class type parameters
- [`KT-23693`](https://youtrack.jetbrains.com/issue/KT-23693) `Add missing actual members` quick fix doesn't work if there is already same-named function with the same signature
- [`KT-23744`](https://youtrack.jetbrains.com/issue/KT-23744) "Kotlin library and Gradle plugin versions are different" inspection false positive for non-JVM dependencies
- [`KT-24492`](https://youtrack.jetbrains.com/issue/KT-24492) "Call on collection type may be reduced" does not change labels from mapNotNull to map
- [`KT-25536`](https://youtrack.jetbrains.com/issue/KT-25536) Use non-const Kotlin 'val' usage in Java code isn't reported on case labels (& assignments)
- [`KT-25933`](https://youtrack.jetbrains.com/issue/KT-25933) ReplaceCallWithBinaryOperator should not suggest to replace 'equals' involving floating-point types
- [`KT-25953`](https://youtrack.jetbrains.com/issue/KT-25953) Meaningless auto properties for Atomic classes
- [`KT-25995`](https://youtrack.jetbrains.com/issue/KT-25995) "Simplify comparision" should try to apply "Simplify if expression" when necessary
- [`KT-26051`](https://youtrack.jetbrains.com/issue/KT-26051) False positive "Redundant visibility modifier" for overridden protected property setter made public
- [`KT-26337`](https://youtrack.jetbrains.com/issue/KT-26337) Exception (resource not found) in quick-fix tests in AS32
- [`KT-26481`](https://youtrack.jetbrains.com/issue/KT-26481) Flaky false positive "Receiver parameter is never used" for local extension function
- [`KT-26571`](https://youtrack.jetbrains.com/issue/KT-26571) Too much highlighting from "convert call chain into sequence"
- [`KT-26650`](https://youtrack.jetbrains.com/issue/KT-26650) False negative "Call chain on collection should be converted into 'Sequence'"" on class implementing `Iterable`
- [`KT-26662`](https://youtrack.jetbrains.com/issue/KT-26662) Corner cases around 'this' inside "replace if with safe access"
- [`KT-26669`](https://youtrack.jetbrains.com/issue/KT-26669) "Remove unnecessary parentheses" reports parens of function returned from extension function
- [`KT-26673`](https://youtrack.jetbrains.com/issue/KT-26673) "Remove parameter" quick fix keeps unused type parameter referred in type constraint
- [`KT-26710`](https://youtrack.jetbrains.com/issue/KT-26710) Should not report "implicit 'it' is shadowed" when outer `it` is not used
- [`KT-26839`](https://youtrack.jetbrains.com/issue/KT-26839) Add braces to if statement produces code that is not formatted according to style
- [`KT-26902`](https://youtrack.jetbrains.com/issue/KT-26902) Bad quickfix name for "Call on non-null type may be reduced"
- [`KT-27016`](https://youtrack.jetbrains.com/issue/KT-27016) Replace 'if' with elvis operator w/ interface generates invalid code (breaks type inference)
- [`KT-27034`](https://youtrack.jetbrains.com/issue/KT-27034) "Redundant SAM constructor" inspection shouldn't make all lambda gray (too much highlighting)
- [`KT-27061`](https://youtrack.jetbrains.com/issue/KT-27061) False positive "Convert to secondary constructor" with delegation
- [`KT-27071`](https://youtrack.jetbrains.com/issue/KT-27071) "Add non-null asserted (!!) call" places `!!` at wrong position with operator `get` (array indexing)
- [`KT-27093`](https://youtrack.jetbrains.com/issue/KT-27093) Create actual class from expect class doesn't add all necessary imports
- [`KT-27104`](https://youtrack.jetbrains.com/issue/KT-27104) False positive "Convert call chain into Sequence" with groupingBy
- [`KT-27116`](https://youtrack.jetbrains.com/issue/KT-27116) "Object literal can be converted to lambda" produces code littered with "return@label"
- [`KT-27138`](https://youtrack.jetbrains.com/issue/KT-27138) Change visibility intentions are suggested on properties marked with @JvmField
- [`KT-27139`](https://youtrack.jetbrains.com/issue/KT-27139) Add getter intention is suggested for properties marked with @JvmField
- [`KT-27146`](https://youtrack.jetbrains.com/issue/KT-27146) False positive "map.put() can be converted to assignment" on `super` keyword with `LinkedHashMap` inheritance
- [`KT-27156`](https://youtrack.jetbrains.com/issue/KT-27156) Introduce backing property intention is suggested for property marked with @JvmField
- [`KT-27157`](https://youtrack.jetbrains.com/issue/KT-27157) Convert property to function intention is suggested for property marked with @JvmField
- [`KT-27173`](https://youtrack.jetbrains.com/issue/KT-27173) "Lift return out of `...`" should work on any of targeted `return` keywords
- [`KT-27184`](https://youtrack.jetbrains.com/issue/KT-27184) "Replace with safe call" is not suggested for nullable var property that is impossible to smart cast
- [`KT-27209`](https://youtrack.jetbrains.com/issue/KT-27209) "Loop parameter 'it' is unused": unhelpful quickfix
- [`KT-27291`](https://youtrack.jetbrains.com/issue/KT-27291) "Create" quick fix: "destination directory" field suggests same root and JVM roots for all platforms
- [`KT-27354`](https://youtrack.jetbrains.com/issue/KT-27354) False positive "Make 'Foo' open" for `data` class inheritance
- [`KT-27408`](https://youtrack.jetbrains.com/issue/KT-27408) "Add braces to 'if' statement" moves end-of-line comment inside an `if` branch
- [`KT-27486`](https://youtrack.jetbrains.com/issue/KT-27486) ConvertCallChainIntoSequence quick fix doesn't convert 'unzip' into 'Sequence'
- [`KT-27539`](https://youtrack.jetbrains.com/issue/KT-27539) False positive `Redundant Companion reference` when val in companion is effectively shadowed by inherited val
- [`KT-27584`](https://youtrack.jetbrains.com/issue/KT-27584) False positive "Move lambda argument out of parentheses" when implementing interface by delegation
- [`KT-27590`](https://youtrack.jetbrains.com/issue/KT-27590) No “Change parameter” quick fix for changing argument type from UInt to Int
- [`KT-27619`](https://youtrack.jetbrains.com/issue/KT-27619) Inspection "Invalid property key" should check whether reference is soft or not
- [`KT-27664`](https://youtrack.jetbrains.com/issue/KT-27664) Fix flaky problem in tests "Could not initialize class UnusedSymbolInspection"
- [`KT-27699`](https://youtrack.jetbrains.com/issue/KT-27699) "Remove redundant spread operator" produces incorrect code
- [`KT-27708`](https://youtrack.jetbrains.com/issue/KT-27708) IDE highlights internal constructors used only from Java as unused
- [`KT-27791`](https://youtrack.jetbrains.com/issue/KT-27791) Don't suggest `Implement as constructor parameters` quick fix for `actual` class declaration
- [`KT-27861`](https://youtrack.jetbrains.com/issue/KT-27861) RedundantCompanionReference false positive for nested class with name "Companion"
- [`KT-27906`](https://youtrack.jetbrains.com/issue/KT-27906) SafeCastAndReturn is not reported on code block with unqualified return
- [`KT-27951`](https://youtrack.jetbrains.com/issue/KT-27951) False declaration in actual list (same name but not really actual)
- [`KT-28047`](https://youtrack.jetbrains.com/issue/KT-28047) False positive "Redundant lambda arrow" for lambda returned from `when` branch
- [`KT-28196`](https://youtrack.jetbrains.com/issue/KT-28196) KotlinAddImportAction: AWT events are not allowed inside write action
- [`KT-28200`](https://youtrack.jetbrains.com/issue/KT-28200) KNPE in TypeUtilsKt.getDataFlowAwareTypes
- [`KT-28268`](https://youtrack.jetbrains.com/issue/KT-28268) Don't suggest "make abstract" quick fix for inline classes
- [`KT-28286`](https://youtrack.jetbrains.com/issue/KT-28286) "Unused symbol" inspection: Interface is reported as "class"
- [`KT-28341`](https://youtrack.jetbrains.com/issue/KT-28341) False positive "Introduce backing property" intention for `const` values
- [`KT-28381`](https://youtrack.jetbrains.com/issue/KT-28381) Forbid "move property to constructor" for expect classes
- [`KT-28382`](https://youtrack.jetbrains.com/issue/KT-28382) Forbid "introduce backing property" for expect classes
- [`KT-28383`](https://youtrack.jetbrains.com/issue/KT-28383) Exception during "move to companion" for expect class member
- [`KT-28443`](https://youtrack.jetbrains.com/issue/KT-28443) "Move out of companion object" intention is suggested for @JvmField property inside companion object of interface
- [`KT-28504`](https://youtrack.jetbrains.com/issue/KT-28504) Redundant async inspection: support calls on explicitly given scope
- [`KT-28540`](https://youtrack.jetbrains.com/issue/KT-28540) "Replace assert boolean with assert equality" inspection quickfix doesn't add import statement
- [`KT-28618`](https://youtrack.jetbrains.com/issue/KT-28618) Kotlin: convert anonymous function to lambda expression failed if no space at start of lambda expression
- [`KT-28694`](https://youtrack.jetbrains.com/issue/KT-28694) "Assign backing field" quick fix adds empty line before created assignment
- [`KT-28716`](https://youtrack.jetbrains.com/issue/KT-28716) KotlinDefaultHighlightingSettingsProvider suppresses inspections in non-kotlin files
- [`KT-28744`](https://youtrack.jetbrains.com/issue/KT-28744) val-keyword went missing from constructor of inline class after applying “Create actual class...” intention
- [`KT-28745`](https://youtrack.jetbrains.com/issue/KT-28745) val-keyword went missing from constructor of inline class after applying “Create expected class in common module...” intention

### IDE. KDoc

- [`KT-24788`](https://youtrack.jetbrains.com/issue/KT-24788) Endless exceptions in offline inspections

### IDE. Multiplatform

- [`KT-26518`](https://youtrack.jetbrains.com/issue/KT-26518) `Create actual ...` quick fix doesn't add a primary constructor call for the actual secondary constructor
- [`KT-26893`](https://youtrack.jetbrains.com/issue/KT-26893) Multiplatform projects fail to import into Android Studio 3.3, 3.4
- [`KT-26957`](https://youtrack.jetbrains.com/issue/KT-26957) Merge expect gutter icon, when used for the same line
- [`KT-27295`](https://youtrack.jetbrains.com/issue/KT-27295) MPP: Rebuild module / Recompile source does nothing for Native with Delegate to gradle = Yes
- [`KT-27296`](https://youtrack.jetbrains.com/issue/KT-27296) MPP: Rebuild module / Recompile source does nothing for Common with Delegate to gradle = Yes
- [`KT-27335`](https://youtrack.jetbrains.com/issue/KT-27335) New multiplatform wizard: mobile library is generated with failed test
- [`KT-27595`](https://youtrack.jetbrains.com/issue/KT-27595) KNPE on attempt to generate `equals()`, `hashCode()`, `toString()` for `expect` class

### IDE. Navigation

- [`KT-22637`](https://youtrack.jetbrains.com/issue/KT-22637) Go to actual declarations for enum values should choose correct value if they are written in one line
- [`KT-27494`](https://youtrack.jetbrains.com/issue/KT-27494) Create tooling tests for new-multiplatform
- [`KT-28206`](https://youtrack.jetbrains.com/issue/KT-28206) Go to implementations on expect enum shows not only enum classes, but also all members
- [`KT-28398`](https://youtrack.jetbrains.com/issue/KT-28398) Broken navigation to actual declaration of `println()` in non-gradle project

### IDE. Project View

- [`KT-26210`](https://youtrack.jetbrains.com/issue/KT-26210) IOE “Cannot create file” on creating new file with existing filename by pasting a code in Project view
- [`KT-27903`](https://youtrack.jetbrains.com/issue/KT-27903) Can create file with empty name without any warning

### IDE. REPL

- [`KT-29285`](https://youtrack.jetbrains.com/issue/KT-29285) Starting REPL in Gradle project: Will compile into IDEA's out folder which then shadows Gradle's compile output

### IDE. Refactorings

- [`KT-23603`](https://youtrack.jetbrains.com/issue/KT-23603) Add the support for find usages/refactoring of the buildSrc sources in gradle kotlin DSL build scripts
- [`KT-26696`](https://youtrack.jetbrains.com/issue/KT-26696) Copy, Move: "Destination directory" field does not allow to choose a path from non-JVM module
- [`KT-28408`](https://youtrack.jetbrains.com/issue/KT-28408) "Extract interface" action should not show private properties
- [`KT-28476`](https://youtrack.jetbrains.com/issue/KT-28476) Extract interface / super class on non-JVM class throws KNPE

### IDE. Scratch

- [`KT-23523`](https://youtrack.jetbrains.com/issue/KT-23523) Filter out fake gradle modules from checkbox in Scratch file panel
- [`KT-25032`](https://youtrack.jetbrains.com/issue/KT-25032) Scratch: IDEA hangs/freezes on code that never returns (infinite loops)
- [`KT-26271`](https://youtrack.jetbrains.com/issue/KT-26271) Scratches for Kotlin  do not work when clicking "Run Scratch File" button
- [`KT-26332`](https://youtrack.jetbrains.com/issue/KT-26332) Fix classpath intention in Kotlin scratch file in Java only project doesn't do anything
- [`KT-27628`](https://youtrack.jetbrains.com/issue/KT-27628) Scratch blocks AWT Queue thread
- [`KT-28045`](https://youtrack.jetbrains.com/issue/KT-28045) 'Run kotlin scratch' is shown for jest tests

### IDE. Script

- [`KT-24465`](https://youtrack.jetbrains.com/issue/KT-24465) Provide a UI to manage script definitions
- [`KT-24466`](https://youtrack.jetbrains.com/issue/KT-24466) Add warning when there are multiple script definitions for one script
- [`KT-25818`](https://youtrack.jetbrains.com/issue/KT-25818) IDE Scripting Console files shouldn't have scratch panel
- [`KT-26331`](https://youtrack.jetbrains.com/issue/KT-26331) Please extract ScriptDefinitionContributor/KotlinScriptDefinition from kotlin-plugin.jar to separate jar
- [`KT-27669`](https://youtrack.jetbrains.com/issue/KT-27669) Consider moving expensive tasks out of the UI thread
- [`KT-27743`](https://youtrack.jetbrains.com/issue/KT-27743) Do not start multiple background threads loading dependencies for different scripts
- [`KT-27817`](https://youtrack.jetbrains.com/issue/KT-27817) Implement a lightweight EP in a separate public jar for supplying script definitions to IDEA
- [`KT-27960`](https://youtrack.jetbrains.com/issue/KT-27960) Add capability to import one Script to another
- [`KT-28046`](https://youtrack.jetbrains.com/issue/KT-28046) "Reload script dependencies on file change" option is missing after project restart

### IDE. Tests Support

- [`KT-27977`](https://youtrack.jetbrains.com/issue/KT-27977) Missing 'run' gutter on a test method of an abstract class
- [`KT-28080`](https://youtrack.jetbrains.com/issue/KT-28080) Wrong run configuration created from context for test method in abstract class

### JS. Tools

- [`KT-27361`](https://youtrack.jetbrains.com/issue/KT-27361) Support NamedConstructor in idl2k
- [`KT-28786`](https://youtrack.jetbrains.com/issue/KT-28786) Float values initialized incorrectly while translating from IDL
- [`KT-28821`](https://youtrack.jetbrains.com/issue/KT-28821) Kotlin/JS missing ClipboardEvent definitions
- [`KT-28864`](https://youtrack.jetbrains.com/issue/KT-28864) Better support for TrackEvent, MediaStreamTrackEvent and RTCTrackEvent in idl

### JavaScript

- [`KT-27611`](https://youtrack.jetbrains.com/issue/KT-27611) Calling a suspending function of a JS library causes "Uncaught ReferenceError: CoroutineImpl is not defined"
- [`KT-28207`](https://youtrack.jetbrains.com/issue/KT-28207) Finally block loops forever for specific code shape
- [`KT-28215`](https://youtrack.jetbrains.com/issue/KT-28215) JS: inline suspend function not usable in non-inlined form
- [`KT-29003`](https://youtrack.jetbrains.com/issue/KT-29003) KotlinJS: Size of String in stdlib is limited if the the Constructor String(chars: CharArray) gets used

### Libraries

#### New Features

- [`KT-18398`](https://youtrack.jetbrains.com/issue/KT-18398) Provide a way for libraries to avoid mixing Kotlin 1.0 and 1.1 dependencies in end user projects
- [`KT-27919`](https://youtrack.jetbrains.com/issue/KT-27919) Publish modularized artifacts under 'modular' classifier

#### Performance Improvements

- [`KT-27251`](https://youtrack.jetbrains.com/issue/KT-27251) Do not use Stack in FileTreeWalk iterator implementation

#### Fixes

- [`KT-12473`](https://youtrack.jetbrains.com/issue/KT-12473) KotlinJS - comparator returning 0 changes order
- [`KT-20743`](https://youtrack.jetbrains.com/issue/KT-20743) Use strongly typed events in Kotlin2js DOM API
- [`KT-20865`](https://youtrack.jetbrains.com/issue/KT-20865) Retrieving groups by name is not supported on Java 9 even with `kotlin-stdlib-jre8` in the classpath
- [`KT-23932`](https://youtrack.jetbrains.com/issue/KT-23932) add "PointerEvent" for kotlin-stdlib-js 
- [`KT-24336`](https://youtrack.jetbrains.com/issue/KT-24336) Kotlin/JS missing SVGMaskElement interface
- [`KT-25371`](https://youtrack.jetbrains.com/issue/KT-25371) Support unsigned integers in kotlinx-metadata-jvm
- [`KT-27629`](https://youtrack.jetbrains.com/issue/KT-27629) kotlin.test BeforeTest/AfterTest annotation mapping for TestNG
- [`KT-28091`](https://youtrack.jetbrains.com/issue/KT-28091) Provide correct AbstractMutableCollections declarations in stdlib-common
- [`KT-28251`](https://youtrack.jetbrains.com/issue/KT-28251) Stdlib: Deprecated ReplaceWith `kotlin.math.log` replacement instead of `kotlin.math.ln`
- [`KT-28488`](https://youtrack.jetbrains.com/issue/KT-28488) Add clarification for COROUTINES_SUSPENDED documentation
- [`KT-28572`](https://youtrack.jetbrains.com/issue/KT-28572) readLine() stumbles at surrogate pairs
- [`KT-29187`](https://youtrack.jetbrains.com/issue/KT-29187) JS toTypedArray returns array of invalid type for LongArray and BooleanArray

### Reflection

- [`KT-26765`](https://youtrack.jetbrains.com/issue/KT-26765) Support calling constructors with inline classes in the signature in reflection
- [`KT-27585`](https://youtrack.jetbrains.com/issue/KT-27585) Flaky IllegalPropertyDelegateAccessException: Cannot obtain the delegate of a non-accessible property. Use "isAccessible = true" to make the property accessible
- [`KT-27598`](https://youtrack.jetbrains.com/issue/KT-27598) "KotlinReflectionInternalError" when using `callBy` on constructor that has inline class parameters
- [`KT-27913`](https://youtrack.jetbrains.com/issue/KT-27913) ReflectJvmMapping.getKotlinFunction(ctor) works incorrectly with types containing variables of inline class

### Tools. CLI

- [`KT-27226`](https://youtrack.jetbrains.com/issue/KT-27226) Argfiles: An empty argument in quotes with a whitespace or a newline after it interrupts further reading of arguments
- [`KT-27430`](https://youtrack.jetbrains.com/issue/KT-27430) [Experimental API] Report warning instead of error if non-marker is used in -Xuse-experimental/-Xexperimental
- [`KT-27626`](https://youtrack.jetbrains.com/issue/KT-27626) -Xmodule-path does not work in Gradle project with Java 9
- [`KT-27709`](https://youtrack.jetbrains.com/issue/KT-27709) Using an experimental API that does not exist should warn, not error
- [`KT-27775`](https://youtrack.jetbrains.com/issue/KT-27775) Re-enable directories passed as <sources> in -Xbuild-file
- [`KT-27930`](https://youtrack.jetbrains.com/issue/KT-27930) Do not use toURI in ModuleVisibilityUtilsKt.isContainedByCompiledPartOfOurModule if possible
- [`KT-28180`](https://youtrack.jetbrains.com/issue/KT-28180) Backslash-separated file paths in argfiles do not work on Windows
- [`KT-28974`](https://youtrack.jetbrains.com/issue/KT-28974) Serialization bug in CommonToolArguments, affecting MPP project data serialization

### Tools. Compiler Plugins

- [`KT-24997`](https://youtrack.jetbrains.com/issue/KT-24997) Pass arguments to Kapt in human-readable format
- [`KT-24998`](https://youtrack.jetbrains.com/issue/KT-24998) Introduce separate command line tool specifically for Kapt in order to improve UX
- [`KT-25128`](https://youtrack.jetbrains.com/issue/KT-25128) ABI jar generation in the CLI compiler for Bazel like build systems.

### Tools. Gradle

#### New Features

- [`KT-26963`](https://youtrack.jetbrains.com/issue/KT-26963) Warn user that a custom platform-agnostic source set wouldn't be included into build unless it is required for other source sets
- [`KT-27394`](https://youtrack.jetbrains.com/issue/KT-27394) Add kotlinOptions in compilations of the new MPP model
- [`KT-27535`](https://youtrack.jetbrains.com/issue/KT-27535) Implement AARs building and publishing in new MPP
- [`KT-27685`](https://youtrack.jetbrains.com/issue/KT-27685) In new MPP, expose a compilation's default source set via DSL
- [`KT-28155`](https://youtrack.jetbrains.com/issue/KT-28155) Add ability to run tasks in parallel within project
- [`KT-28842`](https://youtrack.jetbrains.com/issue/KT-28842) Enable JS IC by default

#### Performance Improvements

- [`KT-24530`](https://youtrack.jetbrains.com/issue/KT-24530) Enable compile avoidance for kaptKotlin tasks
- [`KT-28037`](https://youtrack.jetbrains.com/issue/KT-28037) In-process Kotlin compiler leaks thread local values

#### Fixes

- [`KT-26065`](https://youtrack.jetbrains.com/issue/KT-26065) Kotlin Gradle plugin resolves dependencies at configuration time
- [`KT-26389`](https://youtrack.jetbrains.com/issue/KT-26389) Support Gradle Kotlin DSL in projects with the `kotlin-multiplatform` plugin
- [`KT-26663`](https://youtrack.jetbrains.com/issue/KT-26663) Gradle dependency DSL features missing for the new MPP dependencies
- [`KT-26808`](https://youtrack.jetbrains.com/issue/KT-26808) Deprecation Warning Gradle 5 - "DefaultSourceDirectorySet constructor has been deprecated"
- [`KT-26978`](https://youtrack.jetbrains.com/issue/KT-26978) Gradle verification fails on DiscoverScriptExtensionsTask
- [`KT-27682`](https://youtrack.jetbrains.com/issue/KT-27682) Kotlin MPP DSL: a target is missing the `attributes { ... }` function, only the `attributes` property is available.
- [`KT-27950`](https://youtrack.jetbrains.com/issue/KT-27950) Gradle 5.0-rc1: "Compilation with Kotlin compile daemon was not successful"
- [`KT-28355`](https://youtrack.jetbrains.com/issue/KT-28355) Gradle Kotlin plugin publishes "api" dependencies with runtime scope
- [`KT-28363`](https://youtrack.jetbrains.com/issue/KT-28363) Enable resources processing for Kotlin/JS target in multiplatform projects
- [`KT-28469`](https://youtrack.jetbrains.com/issue/KT-28469) Gradle Plugin: Task DiscoverScriptExtensionsTask is never up-to-date
- [`KT-28482`](https://youtrack.jetbrains.com/issue/KT-28482) Always rewrite the MPP dependencies in POMs, even when publishing with Gradle metadata
- [`KT-28520`](https://youtrack.jetbrains.com/issue/KT-28520) MPP plugin can't be applied altogether with the "maven-publish" plugin in a Gradle 5 build
- [`KT-28635`](https://youtrack.jetbrains.com/issue/KT-28635) fromPreset() in MPP Gradle plugin DSL is hard to use from Gradle Kotlin DSL scripts
- [`KT-28749`](https://youtrack.jetbrains.com/issue/KT-28749) Expose `allKotlinSourceSets` in `KotlinCompilation`
- [`KT-28795`](https://youtrack.jetbrains.com/issue/KT-28795) The localToProject attribute is not properly disambiguated with Gradle 4.10.2+
- [`KT-28836`](https://youtrack.jetbrains.com/issue/KT-28836) Kotlin compiler logs from Gradle Kotlin Plugin aren't captured by Gradle
- [`KT-29058`](https://youtrack.jetbrains.com/issue/KT-29058) Gradle Plugin: Multiplatform project with maven-publish plugin does not use project group for "metadata" artifact POM

### Tools. J2K

- [`KT-26073`](https://youtrack.jetbrains.com/issue/KT-26073) Irrelevant "create extra commit with java->kt rename"

### Tools. JPS

- [`KT-26980`](https://youtrack.jetbrains.com/issue/KT-26980) JPS Native warning is duplicated for test source sets
- [`KT-27285`](https://youtrack.jetbrains.com/issue/KT-27285) MPP: invalid common -> platform dependency: JPS fails with Throwable "Cannot initialize Kotlin context: Cyclically dependent modules" at KotlinChunk.<init>()
- [`KT-27622`](https://youtrack.jetbrains.com/issue/KT-27622) JPS, JS: Resources marked as "kotlin-resource" are not copied to the out folder in a Kotlin-js project
- [`KT-28095`](https://youtrack.jetbrains.com/issue/KT-28095) JPS: support -Xcommon-sources for multiplatform projects (JS)
- [`KT-28316`](https://youtrack.jetbrains.com/issue/KT-28316) Report `Native is not yet supported in IDEA internal build system` on JPS build once per project/multiplatform module
- [`KT-28527`](https://youtrack.jetbrains.com/issue/KT-28527) JPS: Serialization plugin not loaded in ktor
- [`KT-28900`](https://youtrack.jetbrains.com/issue/KT-28900) With "Keep compiler process alive between invocations = No" (disabled daemon) JPS rebuild fails with SCE: "Provider AndroidCommandLineProcessor not a subtype" at PluginCliParser.processPluginOptions()

### Tools. Scripts

- [`KT-27382`](https://youtrack.jetbrains.com/issue/KT-27382) Embeddable version of scripting support (KEEP 75)
- [`KT-27497`](https://youtrack.jetbrains.com/issue/KT-27497) kotlin script -  No class roots are found in the JDK path
- [`KT-29293`](https://youtrack.jetbrains.com/issue/KT-29293) Script compilation - standard libs are not added to the dependencies
- [`KT-29301`](https://youtrack.jetbrains.com/issue/KT-29301) Some ivy resolvers are proguarded out of the kotlin-main-kts
- [`KT-29319`](https://youtrack.jetbrains.com/issue/KT-29319) scripts default jvmTarget causes inlining problems - default should be 1.8

### Tools. kapt

#### New Features

- [`KT-28024`](https://youtrack.jetbrains.com/issue/KT-28024) Kapt: Add option for printing timings for individual annotation processors
- [`KT-28025`](https://youtrack.jetbrains.com/issue/KT-28025) Detect memory leaks in annotation processors

#### Performance Improvements

- [`KT-28852`](https://youtrack.jetbrains.com/issue/KT-28852) Cache classloaders for tools.jar and kapt in Gradle workers

#### Fixes

- [`KT-24368`](https://youtrack.jetbrains.com/issue/KT-24368) Kapt: Do not include compile classpath entries in the annotation processing classpath
- [`KT-25756`](https://youtrack.jetbrains.com/issue/KT-25756) Investigate file descriptors leaks in kapt
- [`KT-26145`](https://youtrack.jetbrains.com/issue/KT-26145) Using `kapt` without the `kotlin-kapt` plugin should throw a build error
- [`KT-26304`](https://youtrack.jetbrains.com/issue/KT-26304) Build fails with "cannot find symbol" using gRPC with dagger; stub compilation fails to find classes generated by kapt
- [`KT-26725`](https://youtrack.jetbrains.com/issue/KT-26725) Kapt does not handle androidx.annotation.RecentlyNullable correctly
- [`KT-26817`](https://youtrack.jetbrains.com/issue/KT-26817) kapt 1.2.60+ ignores .java files that are symlinks
- [`KT-27126`](https://youtrack.jetbrains.com/issue/KT-27126) kapt: class implementing List<T> generates bad stub
- [`KT-27188`](https://youtrack.jetbrains.com/issue/KT-27188) kapt Gradle plugin fails in Java 10+ ("Cannot find tools.jar")
- [`KT-27334`](https://youtrack.jetbrains.com/issue/KT-27334) [Kapt] Stub generator uses constant value in method annotation instead of constant name.
- [`KT-27404`](https://youtrack.jetbrains.com/issue/KT-27404) Kapt does not call annotation processors on custom (e.g., androidTest) source sets if all dependencies are inherited from the main kapt configuration
- [`KT-27487`](https://youtrack.jetbrains.com/issue/KT-27487) Previous value is passed to annotation parameter using annotation processing
- [`KT-27711`](https://youtrack.jetbrains.com/issue/KT-27711) kapt: ArrayIndexOutOfBoundsException: 0
- [`KT-27910`](https://youtrack.jetbrains.com/issue/KT-27910) Kapt lazy stub without explicit type that initializes an object expression breaks stubbing


## 1.3.11

### Compiler

- [`KT-28097`](https://youtrack.jetbrains.com/issue/KT-28097) AbstractMethodError for @JvmSuppressWildcards annotation used with coroutines
- [`KT-28225`](https://youtrack.jetbrains.com/issue/KT-28225) Report a warning when comparing incompatible enums

### IDE. Gradle

- [`KT-28389`](https://youtrack.jetbrains.com/issue/KT-28389) MPP IDE import does not recognize a dependency from a subproject to the root project

### IDE. Inspections and Intentions

- [`KT-28445`](https://youtrack.jetbrains.com/issue/KT-28445) "Redundant async" inspection does not work with release coroutines

### IDE. Multiplatform

- [`KT-27632`](https://youtrack.jetbrains.com/issue/KT-27632) MPP IDE commonMain cannot see other commonMain types transitively which are exposed through a middle multiplatform module

### JavaScript

- [`KT-27946`](https://youtrack.jetbrains.com/issue/KT-27946) Late initialization based on contracts breaks Kotlin/JS in 1.3.0

### Tools. Gradle

- [`KT-27500`](https://youtrack.jetbrains.com/issue/KT-27500) MPP: Native: `.module` dependency is reported as error in the IDE after import


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

## 1.3-M2

### Compiler

#### New Features

- [`KT-6592`](https://youtrack.jetbrains.com/issue/KT-6592) Support local variable initialization in lambda arguments to some functions
- [`KT-19323`](https://youtrack.jetbrains.com/issue/KT-19323) Allow generic type parameter to have mixed constraints for @InlineOnly functions
- [`KT-24857`](https://youtrack.jetbrains.com/issue/KT-24857) Support compatibility wrappers for coroutines in compiler
- [`KT-25320`](https://youtrack.jetbrains.com/issue/KT-25320) Support limited conversions for constants to simplify interop for Kotlin/Native
- [`KT-25372`](https://youtrack.jetbrains.com/issue/KT-25372) Support JvmName on annotation property getters
- [`KT-25495`](https://youtrack.jetbrains.com/issue/KT-25495) Release contract DSL as experimental
- [`KT-25557`](https://youtrack.jetbrains.com/issue/KT-25557) Metadata that maps coroutine label to the file name and line number
- [`KT-25559`](https://youtrack.jetbrains.com/issue/KT-25559) Metadata that maps coroutine spilled state to local variables
- [`KT-25655`](https://youtrack.jetbrains.com/issue/KT-25655) Safe or non-null calls required on x following check(x != null)
- [`KT-25972`](https://youtrack.jetbrains.com/issue/KT-25972) Support reading binary metadata of the next major release

#### Performance Improvements

- [`KT-24657`](https://youtrack.jetbrains.com/issue/KT-24657) Compiler performance issues with big files
- [`KT-26243`](https://youtrack.jetbrains.com/issue/KT-26243) Avoid redundant "specialized" method for suspend lambdas.

#### Fixes

- [`KT-19628`](https://youtrack.jetbrains.com/issue/KT-19628) Unresolved reference not reported on data class constructor `@get` annotation
- [`KT-20830`](https://youtrack.jetbrains.com/issue/KT-20830) Nulls can propagate through "enhanced nullability" types on module boundaries
- [`KT-21240`](https://youtrack.jetbrains.com/issue/KT-21240) Remove suspendCoroutineOrReturn intrinsic from compiler
- [`KT-22379`](https://youtrack.jetbrains.com/issue/KT-22379) Condition of while-loop with break can produce unsound smartcast
- [`KT-23438`](https://youtrack.jetbrains.com/issue/KT-23438) Back-end (JVM) Internal error: Failed to generate function suspendCoroutineOrReturn
- [`KT-23819`](https://youtrack.jetbrains.com/issue/KT-23819) Inline classes: mapping of fully generic classes 
- [`KT-23857`](https://youtrack.jetbrains.com/issue/KT-23857) Annotation with target TYPE is not applicable to TYPE_USE in Java sources
- [`KT-24717`](https://youtrack.jetbrains.com/issue/KT-24717) Allow number literals to be used as unsigned ones with unsigned expected typed
- [`KT-24860`](https://youtrack.jetbrains.com/issue/KT-24860) Forbid usage function expression as suspend function expression
- [`KT-24872`](https://youtrack.jetbrains.com/issue/KT-24872) Do not generate user-defined methods inside box class of inline class
- [`KT-24873`](https://youtrack.jetbrains.com/issue/KT-24873) Generate equals/hashCode/toString methods for inline classes same as for data classes
- [`KT-25246`](https://youtrack.jetbrains.com/issue/KT-25246) Incorrect bytecode generated for secondary constructor in inline class + primitive array
- [`KT-25278`](https://youtrack.jetbrains.com/issue/KT-25278) No smart cast for "returns() implies" contract when default argument is omitted
- [`KT-25287`](https://youtrack.jetbrains.com/issue/KT-25287) Getter-targeted annotations on annotation constructor parameters are lost
- [`KT-25293`](https://youtrack.jetbrains.com/issue/KT-25293) “Couldn't transform method node” error on compiling inline class with hashCode() method call when underlying value type is basic (number, char, boolean)
- [`KT-25299`](https://youtrack.jetbrains.com/issue/KT-25299) NoSuchMethodError Foo$Erased.hashCode(Ljava/lang/Object;) for hashCode(), toString() and equals() methods in inline classes
- [`KT-25328`](https://youtrack.jetbrains.com/issue/KT-25328) “Couldn't transform method node” error on compiling inline class which is wrapping Unit type
- [`KT-25330`](https://youtrack.jetbrains.com/issue/KT-25330) CCE “[Ljava.lang.Integer; cannot be cast to Foo” for inline class which is wrapping Array<T>
- [`KT-25521`](https://youtrack.jetbrains.com/issue/KT-25521) Coroutines state machine in Kotlin 1.3 should not have getLabel/setLabel
- [`KT-25558`](https://youtrack.jetbrains.com/issue/KT-25558) Stabilize field naming and mangling for suspending lambda classes
- [`KT-25580`](https://youtrack.jetbrains.com/issue/KT-25580) No warning about experimental API when unsigned types are inferred
- [`KT-25599`](https://youtrack.jetbrains.com/issue/KT-25599) “Exception during code generation” on compiling code with public constructor of unsigned numbers array
- [`KT-25614`](https://youtrack.jetbrains.com/issue/KT-25614) Support secondary constructors for inline classes
- [`KT-25683`](https://youtrack.jetbrains.com/issue/KT-25683) Compiler support for calling experimental suspend functions and function with experimental suspend function type as parameter
- [`KT-25688`](https://youtrack.jetbrains.com/issue/KT-25688) Add $continuation to LVT
- [`KT-25750`](https://youtrack.jetbrains.com/issue/KT-25750) CCE “Foo cannot be cast to java.lang.String” with inline class
- [`KT-25760`](https://youtrack.jetbrains.com/issue/KT-25760) Inline data class throws java.lang.VerifyError when trying toString() it
- [`KT-25794`](https://youtrack.jetbrains.com/issue/KT-25794) Incorrect code generated for accessing elements of Array<C> where C is inline class
- [`KT-25824`](https://youtrack.jetbrains.com/issue/KT-25824) Move SuspendFunctionN fictitious interfaces to kotlin.coroutines package
- [`KT-25825`](https://youtrack.jetbrains.com/issue/KT-25825) Allow to distinguish instances of function types from instances of suspend function types via kotlin.coroutines.jvm.internal.SuspendFunction marker interface
- [`KT-25912`](https://youtrack.jetbrains.com/issue/KT-25912) Calling groupingBy+reduce from suspend function causes IncompatibleClassChangeError
- [`KT-25914`](https://youtrack.jetbrains.com/issue/KT-25914) '==' for inline class with custom 'equals' uses underlying primitive type comparison instead
- [`KT-25973`](https://youtrack.jetbrains.com/issue/KT-25973) Report metadata version mismatch upon discovering a .kotlin_module file in the dependencies with an incompatible metadata version
- [`KT-25981`](https://youtrack.jetbrains.com/issue/KT-25981) Incorrect code generated for unboxed to boxed inline class equality
- [`KT-25983`](https://youtrack.jetbrains.com/issue/KT-25983) Inline class equality uses IEEE 754 instead of total order
- [`KT-26029`](https://youtrack.jetbrains.com/issue/KT-26029) Prohibit delegated properties inside inline classes
- [`KT-26030`](https://youtrack.jetbrains.com/issue/KT-26030) Prohibit implementation by delegation for inline classes
- [`KT-26052`](https://youtrack.jetbrains.com/issue/KT-26052) Inline Classes: IllegalArgumentException when underlying type is non-null but declared type is nullable
- [`KT-26101`](https://youtrack.jetbrains.com/issue/KT-26101) Prohibit inline classes with recursive underlying types
- [`KT-26103`](https://youtrack.jetbrains.com/issue/KT-26103) Inline class with type parameters is inconsistently mapped to JVM type if underlying type is a primitive
- [`KT-26120`](https://youtrack.jetbrains.com/issue/KT-26120) Inline Classes: Class inheritance is allowed but fails when referencing a superclass member

### IDE

- [`KT-25316`](https://youtrack.jetbrains.com/issue/KT-25316) PARTIAL resolve mode doesn't work when effects system is enabled
- [`KT-25611`](https://youtrack.jetbrains.com/issue/KT-25611) With Language / API version = "Latest stable" installing 1.3-M1 plugin upgrades actual values to 1.3
- [`KT-25681`](https://youtrack.jetbrains.com/issue/KT-25681) Remove "Coroutines (experimental)" settings from IDE and do not pass `-Xcoroutines` to JPS compiler (since 1.3)
- [`KT-25714`](https://youtrack.jetbrains.com/issue/KT-25714) Kotlin plugin updater suggests plugins incompatible with current Studio build platform
- [`KT-26239`](https://youtrack.jetbrains.com/issue/KT-26239) New MPP template: add `kotlin-test` libraries to the modules dependencies by default
- [`KT-26290`](https://youtrack.jetbrains.com/issue/KT-26290) Gradle Import: When all modules have the same language/API version use it for project-level settings as well

### IDE. Completion

- [`KT-25275`](https://youtrack.jetbrains.com/issue/KT-25275) Code completion does not take into account smart casts gotten from "returns implies" contract

### IDE. Inspections and Intentions

- [`KT-22330`](https://youtrack.jetbrains.com/issue/KT-22330) "Add remaining branch" quickfix doesn't properly import enum class
- [`KT-22354`](https://youtrack.jetbrains.com/issue/KT-22354) "Add remaining branches with import" quick fix causes KNPE at ImportAllMembersIntention$Companion.importReceiverMembers()
- [`KT-26158`](https://youtrack.jetbrains.com/issue/KT-26158) KNPE in "Create local variable"

### IDE. Multiplatform

- [`KT-26217`](https://youtrack.jetbrains.com/issue/KT-26217) "org.jetbrains.kotlin.resolve.MultiTargetPlatform$Common cannot be cast to org.jetbrains.kotlin.resolve.MultiTargetPlatform$Specific" on splitted actuals

### IDE. Script

- [`KT-25814`](https://youtrack.jetbrains.com/issue/KT-25814) IDE scripting console -> kotlin (JSR-223) - compilation errors - unresolved IDEA classes
- [`KT-25822`](https://youtrack.jetbrains.com/issue/KT-25822) jvmTarget from the script compiler options is ignored in the IDE

### JavaScript

- [`KT-22053`](https://youtrack.jetbrains.com/issue/KT-22053) JS: Secondary constructor of Throwable inheritor doesn't call to primary one
- [`KT-25014`](https://youtrack.jetbrains.com/issue/KT-25014) Support 'when' with subject variable in JS back-end
- [`KT-26064`](https://youtrack.jetbrains.com/issue/KT-26064) JS inliner calls wrong constructor in incremental build
- [`KT-26117`](https://youtrack.jetbrains.com/issue/KT-26117) JS runtime error: ArrayList_init instead of ArrayList_init_0
- [`KT-26138`](https://youtrack.jetbrains.com/issue/KT-26138) JS: prohibit external inline class
- [`KT-26171`](https://youtrack.jetbrains.com/issue/KT-26171) Prohibit inline classes as parameter and return type of external declaration

### Language design

- [`KT-7566`](https://youtrack.jetbrains.com/issue/KT-7566) Annotate kotlin.test.Test.assertNotNull() so that safe dereference isn't required after it
- [`KT-14397`](https://youtrack.jetbrains.com/issue/KT-14397) Make "smart cast" to non-null string working after isNullOrEmpty() check.
- [`KT-19532`](https://youtrack.jetbrains.com/issue/KT-19532) Evaluation order for constructor call
- [`KT-22274`](https://youtrack.jetbrains.com/issue/KT-22274) Restrict statement labels applicability

### Libraries

#### New Features

- [`KT-13814`](https://youtrack.jetbrains.com/issue/KT-13814) keys.associateWith { k -> v } function
- [`KT-15539`](https://youtrack.jetbrains.com/issue/KT-15539) Random Convenience Method in Ranges (LongRange, IntRange, etc)
- [`KT-15695`](https://youtrack.jetbrains.com/issue/KT-15695) String/Collection/Map/Array/Sequence.ifEmpty { null }
- [`KT-18986`](https://youtrack.jetbrains.com/issue/KT-18986) Debug-friendly toString implementation for CoroutineImpl
- [`KT-25570`](https://youtrack.jetbrains.com/issue/KT-25570) Random extensions to generate unsigned random numbers
- [`KT-25659`](https://youtrack.jetbrains.com/issue/KT-25659) Consider adding SuccessOrFailure.getOrDefault function
- [`KT-25874`](https://youtrack.jetbrains.com/issue/KT-25874) Support array copying between two existing arrays
- [`KT-25875`](https://youtrack.jetbrains.com/issue/KT-25875) Need more access to raw underlying array in unsigned arrays
- [`KT-25962`](https://youtrack.jetbrains.com/issue/KT-25962) Add contract for 'synchronized'
- [`KT-26339`](https://youtrack.jetbrains.com/issue/KT-26339) Introduce CoroutineStackFrame interface for coroutine stack reconstruction in debugger

#### Fixes

- [`KT-16097`](https://youtrack.jetbrains.com/issue/KT-16097) Index overflow when sequence has more than Int.MAX_VALUE elements
- [`KT-17176`](https://youtrack.jetbrains.com/issue/KT-17176) Long/Int progressions with Long.MIN_VALUE and Int.MIN_VALUE step are weird
- [`KT-19305`](https://youtrack.jetbrains.com/issue/KT-19305) IOStreams#readBytes is badly named
- [`KT-19489`](https://youtrack.jetbrains.com/issue/KT-19489) Array.copyOfRange returns value violating declared type when bounds are out of range
- [`KT-21049`](https://youtrack.jetbrains.com/issue/KT-21049) Different behavior in split by regex at JVM and JS
- [`KT-23799`](https://youtrack.jetbrains.com/issue/KT-23799) Discontinue deprecated artifacts distribution: kotlin-runtime, kotlin-jslib, kotlin-stdlib-jre7/8
- [`KT-25274`](https://youtrack.jetbrains.com/issue/KT-25274) contract() function has internal visibility
- [`KT-25303`](https://youtrack.jetbrains.com/issue/KT-25303) checkNotNull(T?) has no contract in contrast to checkNotNull(T?, () -> Any)
- [`KT-25771`](https://youtrack.jetbrains.com/issue/KT-25771) SuccessOrFailure.isFailure always returns false when boxed
- [`KT-25961`](https://youtrack.jetbrains.com/issue/KT-25961) Provide a way to create default-initialized 'U*Array'
- [`KT-26161`](https://youtrack.jetbrains.com/issue/KT-26161) String-to-number and number-to-string conversions for unsigned integers

### Reflection

- [`KT-14657`](https://youtrack.jetbrains.com/issue/KT-14657) Reflection: Provide ability to enumerate all cases of a sealed class
- [`KT-16795`](https://youtrack.jetbrains.com/issue/KT-16795) KType does not implement KAnnotatedElement
- [`KT-21972`](https://youtrack.jetbrains.com/issue/KT-21972) Reflection: Implement suspend functions `KCallable.callSuspend`,  `callSuspendBy`, and `isSuspend`

### Tools. CLI

- [`KT-25823`](https://youtrack.jetbrains.com/issue/KT-25823) Report a warning or error if an old language version or API version is used

### Tools. Gradle

- [`KT-26301`](https://youtrack.jetbrains.com/issue/KT-26301) In new MPP, a project with no `java` or `java-base` plugin applied cannot depend on a published MPP lib or run tests

### Tools. Scripts

- [`KT-26142`](https://youtrack.jetbrains.com/issue/KT-26142) update maven-central remote repository url



## 1.3-M1 IDE plugins update

### IDE

- Kotlin plugin for Android Studio 3.3 Canary 4 is ready
- [`KT-25713`](https://youtrack.jetbrains.com/issue/KT-25713) Android Studio on 182 platform: Gradle project re-import fails 
- [`KT-25733`](https://youtrack.jetbrains.com/issue/KT-25733) "Convert Java File to Kotlin File" action was always disabled



## 1.3-M1

### Language design

- [`KEEP-104`](https://github.com/Kotlin/KEEP/issues/104) Inline classes
- [`KEEP-135`](https://github.com/Kotlin/KEEP/issues/135) Unsigned integer types
- [`KEEP-95`](https://github.com/Kotlin/KEEP/issues/95) Experimental API annotations
- [`KT-4895`](https://youtrack.jetbrains.com/issue/KT-4895) Support assignment of "when" subject to a variable
- [`KT-13762`](https://youtrack.jetbrains.com/issue/KT-13762) Prohibit annotations with target 'EXPRESSION' and retention 'BINARY' or 'RUNTIME'
- [`KT-16681`](https://youtrack.jetbrains.com/issue/KT-16681) kotlin allows mutating the field of read-only property
- [`KT-21515`](https://youtrack.jetbrains.com/issue/KT-21515) Restrict visibility of classifiers, nested in companion objects

### Compiler

- [`KT-24848`](https://youtrack.jetbrains.com/issue/KT-24848) Refine loading Java overrides of Kotlin suspend functions
- [`KT-9580`](https://youtrack.jetbrains.com/issue/KT-9580) Report an error if 'setparam' target does not make sense for a parameter declaration
- [`KT-16310`](https://youtrack.jetbrains.com/issue/KT-16310) Nested classes inside enum entries capturing outer members
- [`KT-17981`](https://youtrack.jetbrains.com/issue/KT-17981) Type parameter for catch parameter possible when exception is nested in generic, but fails in runtime
- [`KT-21354`](https://youtrack.jetbrains.com/issue/KT-21354) Inconsistent behavior of 'for-in-range' loop if range is an array variable modified in loop body
- [`KT-25333`](https://youtrack.jetbrains.com/issue/KT-25333) Restrict visibility of Java static members from supertypes of companion object
- [`KT-25623`](https://youtrack.jetbrains.com/issue/KT-25623) Do not load experimental coroutines as non-suspend function with additional Continuation parameter

#### Backend. JVM

- [`KT-6301`](https://youtrack.jetbrains.com/issue/KT-6301) Support JvmStatic annotation on interface companion object members
- [`KT-25508`](https://youtrack.jetbrains.com/issue/KT-25508) Inject probeCoroutineSuspended to coroutines body
- [`KT-18987`](https://youtrack.jetbrains.com/issue/KT-18987) Unroll recursion in CoroutineImpl.resume
- [`KT-11567`](https://youtrack.jetbrains.com/issue/KT-11567) Companion object INSTANCE field more visible than companion object class itself
- [`KT-13764`](https://youtrack.jetbrains.com/issue/KT-13764) Support lambdas and function references for arities bigger than 22
- [`KT-16615`](https://youtrack.jetbrains.com/issue/KT-16615) Do not generate ConstantValue attribute for non-const vals
- [`KT-25193`](https://youtrack.jetbrains.com/issue/KT-25193) Names of parameters from Java interface methods implemented by delegation are lost  
- [`KT-25324`](https://youtrack.jetbrains.com/issue/KT-25324) VerifyError “Bad type on operand stack” on running code with call of array iterator for array of inline classes
- [`KT-25325`](https://youtrack.jetbrains.com/issue/KT-25325) CCE “Foo cannot be cast to java.lang.String” for iterating over the list of inline classes
- [`KT-25626`](https://youtrack.jetbrains.com/issue/KT-25626) Inline class values inside string literals don't use their own toString implementation

#### Backend. JVM. Coroutines

- [`KT-20219`](https://youtrack.jetbrains.com/issue/KT-20219) Inline suspend function can't be used as non-inline function
- [`KT-24863`](https://youtrack.jetbrains.com/issue/KT-24863) Support new Continuation API in JVM BE
- [`KT-24864`](https://youtrack.jetbrains.com/issue/KT-24864) Create new CoroutineImpl and other internal coroutines interfaces

#### Backend. JVM. Inline

- [`KT-25511`](https://youtrack.jetbrains.com/issue/KT-25511) Inline classes fail with cross-inline functions

#### Binary Metadata

- [`KT-24617`](https://youtrack.jetbrains.com/issue/KT-24617) Optional expected annotation is unresolved in a dependent platform module
- [`KT-25120`](https://youtrack.jetbrains.com/issue/KT-25120) RequireKotlin on nested class and its members is not loaded correctly
- [`KT-25273`](https://youtrack.jetbrains.com/issue/KT-25273) java.lang.UnsupportedOperationException from incremental JS compilation
- [`KT-25310`](https://youtrack.jetbrains.com/issue/KT-25310) Write isUnsigned flag into metadata for unsigned types


#### Frontend

- [`KT-15807`](https://youtrack.jetbrains.com/issue/KT-15807) @JvmField is not applicable to interface companion properties
- [`KT-16962`](https://youtrack.jetbrains.com/issue/KT-16962) Annotation classes cannot contain types or static fields because they cannot have a body
- [`KT-23153`](https://youtrack.jetbrains.com/issue/KT-23153) Compiler allows to set non constant value as annotation parameter
- [`KT-23362`](https://youtrack.jetbrains.com/issue/KT-23362) Move coroutines to package kolin.coroutines for 1.3
- [`KT-24861`](https://youtrack.jetbrains.com/issue/KT-24861) Fix loading kotlin.suspend compiled with LV=1.2 when release coroutines package is used
- [`KT-25241`](https://youtrack.jetbrains.com/issue/KT-25241) Kotlin compiler doesn't warn about usage of lambda/reference with more than 22 parameters
- [`KT-25600`](https://youtrack.jetbrains.com/issue/KT-25600) NSEE “Collection is empty” after trying to call default constructor of unsigned number class

#### Frontend. Data-flow analysis

- [`KT-22517`](https://youtrack.jetbrains.com/issue/KT-22517) Deprecate smartcasts for local delegated properties

#### Frontend. Declarations

- [`KT-19618`](https://youtrack.jetbrains.com/issue/KT-19618) Data class `copy()` call with optional parameters leads to broken code when the class implements an interface with `copy()`
- [`KT-23277`](https://youtrack.jetbrains.com/issue/KT-23277) Prohibit local annotation classes
- [`KT-24197`](https://youtrack.jetbrains.com/issue/KT-24197) Make 'mod' operator error in 1.3

#### Frontend. Lexer & Parser

- [`KT-24663`](https://youtrack.jetbrains.com/issue/KT-24663) Add 'UL' suffix to represent number literals of unsigned Long type

#### Frontend. Resolution and Inference

- [`KT-16908`](https://youtrack.jetbrains.com/issue/KT-16908) Support callable references to suspending functions
- [`KT-20588`](https://youtrack.jetbrains.com/issue/KT-20588) Report error on single element assignment to varargs in named form in annotations
- [`KT-20589`](https://youtrack.jetbrains.com/issue/KT-20589) Report error on single element assignment to varargs in named form in functions
- [`KT-24859`](https://youtrack.jetbrains.com/issue/KT-24859) Disallow calls of functions annotated with receiver annotated with @RestrictsSuspension in foreign suspension context

### IDE

- [`KT-25466`](https://youtrack.jetbrains.com/issue/KT-25466) Make coroutines resolve to be independent of language and API versions set in `Kotlin Compiler` settings

#### IDE. Inspections and Intentions

- [`KT-24243`](https://youtrack.jetbrains.com/issue/KT-24243) Support quick fix to enable usages of Experimental/UseExperimental
- [`KT-11154`](https://youtrack.jetbrains.com/issue/KT-11154) Spell checking inspection is not suppressable
- [`KT-25169`](https://youtrack.jetbrains.com/issue/KT-25169) Impossible to suppress UAST/JVM inspections

#### IDE. Libraries

- [`KT-25129`](https://youtrack.jetbrains.com/issue/KT-25129) Idea freezes when Kotlin plugin tries to determine if jar is js lib in jvm module


### Libraries

#### New Features

- [`KEEP-131`](https://github.com/Kotlin/KEEP/issues/131), [`KT-17261`](https://youtrack.jetbrains.com/issue/KT-17261) Add random number generator to stdlib and related collection extension functions
- [`KT-7922`](https://youtrack.jetbrains.com/issue/KT-7922) Companion object for Boolean
- [`KT-8247`](https://youtrack.jetbrains.com/issue/KT-8247) Byte size for primitives
- [`KT-16552`](https://youtrack.jetbrains.com/issue/KT-16552) Add Sequence.orEmpty()
- [`KT-18559`](https://youtrack.jetbrains.com/issue/KT-18559) Make SafeContinuation and context impl classes serializable
- [`KT-18910`](https://youtrack.jetbrains.com/issue/KT-18910) StringBuilder#setLength(0) in non-JVM Kotlin
- [`KT-21763`](https://youtrack.jetbrains.com/issue/KT-21763) Provide Char MIN_VALUE/MAX_VALUE constants
- [`KT-23279`](https://youtrack.jetbrains.com/issue/KT-23279) isNullOrEmpty() for collections, maps, and arrays
- [`KT-23602`](https://youtrack.jetbrains.com/issue/KT-23602) Make kotlin.Metadata public

#### Fixes

- [`KT-23564`](https://youtrack.jetbrains.com/issue/KT-23564) KotlinJS: Math is deprecated yet random is not a global function
- [`KT-24856`](https://youtrack.jetbrains.com/issue/KT-24856) Create compatibility wrappers for migration from 1.2 to 1.3 coroutines
- [`KT-24862`](https://youtrack.jetbrains.com/issue/KT-24862) Create new Continuation API for coroutines 

### Reflection

- [`KT-25541`](https://youtrack.jetbrains.com/issue/KT-25541) Incorrect parameter names in reflection for inner class constructor from Java class compiled with "-parameters"
