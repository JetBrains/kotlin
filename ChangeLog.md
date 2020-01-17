# CHANGELOG

## 1.3.70

### Compiler

#### New Features

- [`KT-7745`](https://youtrack.jetbrains.com/issue/KT-7745) Support named arguments in their own position even if the result appears as mixed
- [`KT-34847`](https://youtrack.jetbrains.com/issue/KT-34847) Lift restrictions from `kotlin.Result`

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
- [`KT-31653`](https://youtrack.jetbrains.com/issue/KT-31653) Incorrect transformation of the try-catch cover when inlining
- [`KT-31975`](https://youtrack.jetbrains.com/issue/KT-31975) No diagnostic on error type
- [`KT-32106`](https://youtrack.jetbrains.com/issue/KT-32106) New type inference algorithm: IDE shows error but the code compiles succesfully
- [`KT-32138`](https://youtrack.jetbrains.com/issue/KT-32138) New type inference:  invoking type-aliased extension function red in IDE, but compiles
- [`KT-32168`](https://youtrack.jetbrains.com/issue/KT-32168) Problem in IDE with new type inference and delegate provider
- [`KT-32243`](https://youtrack.jetbrains.com/issue/KT-32243) New type inference fails
- [`KT-32345`](https://youtrack.jetbrains.com/issue/KT-32345) New type inference error when using helper method to create delegate provider
- [`KT-32415`](https://youtrack.jetbrains.com/issue/KT-32415) Type mismatch on argument of super constructor of inner class call
- [`KT-32423`](https://youtrack.jetbrains.com/issue/KT-32423) NI: IllegalStateException: Error type encountered: org.jetbrains.kotlin.types.ErrorUtils$UninferredParameterTypeConstructor@211a538e (ErrorType)
- [`KT-32456`](https://youtrack.jetbrains.com/issue/KT-32456) NI: "IllegalStateException: Error type encountered" when adding emptyList to mutableList
- [`KT-32499`](https://youtrack.jetbrains.com/issue/KT-32499) Kotlin/JS - new type inference with toTypedArray() failure
- [`KT-32742`](https://youtrack.jetbrains.com/issue/KT-32742) Gradle/JS "Unresolved Reference" when accessing setting field of Dynamic object w/ React
- [`KT-32818`](https://youtrack.jetbrains.com/issue/KT-32818) Type inference failed with elvis operator
- [`KT-32862`](https://youtrack.jetbrains.com/issue/KT-32862) NI: Compilation error "IllegalArgumentException: ClassicTypeSystemContextForCS couldn't handle" with overloaded generic extension function reference passed as parameter
- [`KT-33033`](https://youtrack.jetbrains.com/issue/KT-33033) NI: Nothing incorrectly inferred as return type when null passed to generic function with expression if statement body
- [`KT-33197`](https://youtrack.jetbrains.com/issue/KT-33197) Expression with branch resolving to List<…> ultimately resolves to MutableList<…>
- [`KT-33263`](https://youtrack.jetbrains.com/issue/KT-33263) "IllegalStateException: Type variable TypeVariable(T) should not be fixed!" with generic extension function and in variance
- [`KT-33592`](https://youtrack.jetbrains.com/issue/KT-33592) NI: Missed error in IDE — Unsupported [Collection literals outside of annotations]
- [`KT-33932`](https://youtrack.jetbrains.com/issue/KT-33932) Compiler fails when it encounters inaccessible classes in javac integration mode
- [`KT-34029`](https://youtrack.jetbrains.com/issue/KT-34029) StackOverflowError for access to nested object inheriting from containing generic class at `org.jetbrains.kotlin.descriptors.impl.LazySubstitutingClassDescriptor.getTypeConstructor`
- [`KT-34282`](https://youtrack.jetbrains.com/issue/KT-34282) Missing diagnostic of unresolved for callable references with overload resolution ambiguity
- [`KT-34391`](https://youtrack.jetbrains.com/issue/KT-34391) IDE, NI: False negative EXPERIMENTAL_API_USAGE_ERROR with callable reference
- [`KT-34500`](https://youtrack.jetbrains.com/issue/KT-34500) CompilationException when loop range is DoubleArray and loop parameter is casted to super-type (e.g. Any, Number, etc.)
- [`KT-34647`](https://youtrack.jetbrains.com/issue/KT-34647) Gradually rename experimentality annotations
- [`KT-34649`](https://youtrack.jetbrains.com/issue/KT-34649) Deprecate -Xexperimental flag
- [`KT-34743`](https://youtrack.jetbrains.com/issue/KT-34743) Support trailing comma in the compiler
- [`KT-34786`](https://youtrack.jetbrains.com/issue/KT-34786) Flaky type inference for lambda expressions
- [`KT-34820`](https://youtrack.jetbrains.com/issue/KT-34820) NI: Red code when expanding type-aliased extension function in LHS position of elvis
- [`KT-35101`](https://youtrack.jetbrains.com/issue/KT-35101) "AssertionError: Mapping ranges should be presented in inline lambda" with a callable reference argument to inline lambda
- [`KT-35168`](https://youtrack.jetbrains.com/issue/KT-35168) NI: "UninitializedPropertyAccessException: lateinit property subResolvedAtoms has not been initialized"
- [`KT-35172`](https://youtrack.jetbrains.com/issue/KT-35172) NI: False positive type mismatch if nullable type after elvis and safe call inside lambda is returning (expected type is specified explicitly)
- [`KT-35224`](https://youtrack.jetbrains.com/issue/KT-35224) NI: Java call candidate with varargs as Array<something> isn't present if SAM type was used in this call
- [`KT-35426`](https://youtrack.jetbrains.com/issue/KT-35426) `IncompatibleClassChangeError: Method 'int java.lang.Object.hashCode()' must be Methodref constant` when invoking on super with explicit  generic type

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
- [`KT-34956`](https://youtrack.jetbrains.com/issue/KT-34956) UI Freeze: PlainTextPasteImportResolver
- [`KT-35121`](https://youtrack.jetbrains.com/issue/KT-35121) Add support for KtSecondaryConstructors into incremental analysis
- [`KT-35189`](https://youtrack.jetbrains.com/issue/KT-35189) Support incremental analysis of comment and kdoc
- [`KT-35590`](https://youtrack.jetbrains.com/issue/KT-35590) UI freeze in kotlin.idea.core.script.ScriptConfigurationMemoryCache when editing file

#### Fixes

- [`KT-10478`](https://youtrack.jetbrains.com/issue/KT-10478)  Move-statement doesn't work for methods with single-expression body and lambda as returning type
- [`KT-13344`](https://youtrack.jetbrains.com/issue/KT-13344) Reduce visual distraction of val keyword
- [`KT-14758`](https://youtrack.jetbrains.com/issue/KT-14758) Move statement up shouldn't move top level declarations above package and import directives
- [`KT-23305`](https://youtrack.jetbrains.com/issue/KT-23305) We should be able to see platform-specific errors in common module
- [`KT-27806`](https://youtrack.jetbrains.com/issue/KT-27806) UAST: @Deprecated(level=DeprecationLevel.HIDDEN) makes method disappear
- [`KT-28708`](https://youtrack.jetbrains.com/issue/KT-28708) Java IDE fails to understand @JvmDefault on properties from binaries
- [`KT-30489`](https://youtrack.jetbrains.com/issue/KT-30489) Kotlin functions are represented in UAST as UAnnotationMethods
- [`KT-31037`](https://youtrack.jetbrains.com/issue/KT-31037) Lambda expression default parameter 'it' sometimes is not highlighted in a call chain
- [`KT-31365`](https://youtrack.jetbrains.com/issue/KT-31365) IDE does not resolve references to stdlib symbols in certain packages (kotlin.jvm) when using OSGi bundle
- [`KT-32031`](https://youtrack.jetbrains.com/issue/KT-32031) UAST: Method body missing for suspend functions
- [`KT-32540`](https://youtrack.jetbrains.com/issue/KT-32540) UltraLight support for compiler plugins
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

### IDE. Code Style, Formatting

#### New Features

- [`KT-35088`](https://youtrack.jetbrains.com/issue/KT-35088) Insert empty line between a declaration and declaration with comment
- [`KT-35106`](https://youtrack.jetbrains.com/issue/KT-35106) Insert an empty line between a declaration and declaration with annotation

#### Fixes

- [`KT-4194`](https://youtrack.jetbrains.com/issue/KT-4194) Code formatter should not move the end of line comment after `if` condition to the next line
- [`KT-12490`](https://youtrack.jetbrains.com/issue/KT-12490) Formatter inserts empty line between single-line declarations in the presence of a comment
- [`KT-22273`](https://youtrack.jetbrains.com/issue/KT-22273) Labeled statements are formatted incorrectly
- [`KT-22362`](https://youtrack.jetbrains.com/issue/KT-22362) Formatter breaks up infix function used in elvis operator
- [`KT-23811`](https://youtrack.jetbrains.com/issue/KT-23811) Formatter: Constructor parameters are joined with the previous line if prefixed with an annotation
- [`KT-23929`](https://youtrack.jetbrains.com/issue/KT-23929) Formatter: chained method calls: "Chop down if long" setting is ignored
- [`KT-23957`](https://youtrack.jetbrains.com/issue/KT-23957) Formatter tears comments away from file annotations
- [`KT-30393`](https://youtrack.jetbrains.com/issue/KT-30393) Remove unnecessary whitespaces between property accessor and its parameter list in formatter
- [`KT-31881`](https://youtrack.jetbrains.com/issue/KT-31881) Redundant indent for single-line comments inside lambda
- [`KT-32277`](https://youtrack.jetbrains.com/issue/KT-32277) Space before by delegate keyword on a property is not formatted
- [`KT-32324`](https://youtrack.jetbrains.com/issue/KT-32324) Formatter doesn't insert space after safe cast operator `as?`
- [`KT-33553`](https://youtrack.jetbrains.com/issue/KT-33553) Formater does not wrap function chained expression body despite "chained function calls" settings
- [`KT-34049`](https://youtrack.jetbrains.com/issue/KT-34049) Formatter breaks string inside template expression with elvis operator
- [`KT-35093`](https://youtrack.jetbrains.com/issue/KT-35093) Formatter inserts empty line between single-line declarations in the presence of an annotation
- [`KT-35199`](https://youtrack.jetbrains.com/issue/KT-35199) Wrong formatting for lambdas in chain calls

### IDE. Completion

#### Fixes

- [`KT-15286`](https://youtrack.jetbrains.com/issue/KT-15286) Support import auto-completion for extension functions declared in objects
- [`KT-25732`](https://youtrack.jetbrains.com/issue/KT-25732) "null" keyword should have priority in completion sort
- [`KT-29926`](https://youtrack.jetbrains.com/issue/KT-29926) Suggest lambda parameter names in IDE to improve DSL adoption
- [`KT-31762`](https://youtrack.jetbrains.com/issue/KT-31762) Completion: Parameter name is suggested instead of enum entry in entry constructor
- [`KT-32615`](https://youtrack.jetbrains.com/issue/KT-32615) PIEAE for smart completion of anonymous function with importing name inside of function
- [`KT-33979`](https://youtrack.jetbrains.com/issue/KT-33979) No completion for functions from nested objects
- [`KT-34150`](https://youtrack.jetbrains.com/issue/KT-34150) No completion for object methods that override something
- [`KT-34386`](https://youtrack.jetbrains.com/issue/KT-34386) Typo in Kotlin arg postfix completion
- [`KT-34414`](https://youtrack.jetbrains.com/issue/KT-34414) Completion works differently for suspend and regular lambda functions
- [`KT-34644`](https://youtrack.jetbrains.com/issue/KT-34644) Code completion list sorting: do not put method before "return" keyword
- [`KT-35042`](https://youtrack.jetbrains.com/issue/KT-35042) Selecting completion variant works differently for suspend and regular lambda parameter

### IDE. Debugger

- [`KT-12242`](https://youtrack.jetbrains.com/issue/KT-12242) Debugger: breakpoint in a class is not hit if the class was first accessed in Evaluate Expression
- [`KT-20342`](https://youtrack.jetbrains.com/issue/KT-20342) Step Over jumps to the wrong position (KotlinUFile)
- [`KT-30909`](https://youtrack.jetbrains.com/issue/KT-30909) "Kotlin variables" button looks inconsistent with panel style
- [`KT-32704`](https://youtrack.jetbrains.com/issue/KT-32704) ISE "Descriptor can be left only if it is last "on calling function with expression body inside Evaluate Expression window
- [`KT-32736`](https://youtrack.jetbrains.com/issue/KT-32736) Evaluate Expression on statement makes error or shows nothing
- [`KT-32741`](https://youtrack.jetbrains.com/issue/KT-32741) "Anonymous functions with names are prohibited "on evaluating functions in Expression mode
- [`KT-33303`](https://youtrack.jetbrains.com/issue/KT-33303) "Smart step into" doesn't work for library declarations
- [`KT-33304`](https://youtrack.jetbrains.com/issue/KT-33304) Can't put a breakpoint to the first line in a file
- [`KT-33728`](https://youtrack.jetbrains.com/issue/KT-33728) Smart Step Into doesn't work for @InlineOnly functions
- [`KT-35316`](https://youtrack.jetbrains.com/issue/KT-35316) IndexNotReadyException on function breakpoint

### IDE. Folding

- [`KT-6316`](https://youtrack.jetbrains.com/issue/KT-6316) Folding of multiline functions which don't have curly braces (expression-body functions)

### IDE. Gradle. Script

- [`KT-31976`](https://youtrack.jetbrains.com/issue/KT-31976) Adding a space in build.gradle.kts leads to 'Gradle projects need to be imported' notification
- [`KT-34441`](https://youtrack.jetbrains.com/issue/KT-34441) *.gradle.kts: load all scripts configuration at project import
- [`KT-34442`](https://youtrack.jetbrains.com/issue/KT-34442) *.gradle.kts: avoid just-in-case script configuration request to Gradle
- [`KT-34530`](https://youtrack.jetbrains.com/issue/KT-34530) Equal duplicate script definitions are listed three times in Preferences
- [`KT-34740`](https://youtrack.jetbrains.com/issue/KT-34740) Implement completion for implicit receivers in scripts with new scripting API
- [`KT-35096`](https://youtrack.jetbrains.com/issue/KT-35096) Duplicated “Kotlin Script” definition for Gradle/Kotlin projects
- [`KT-35149`](https://youtrack.jetbrains.com/issue/KT-35149) build.graldle.kts settings importing: configuration for buildSrc/prepare-deps/build.gradle.kts not loaded
- [`KT-35205`](https://youtrack.jetbrains.com/issue/KT-35205) *.gradle.kts: avoid just-in-case script configuration request to Gradle while loading from FS

### IDE. Hints. Parameter Info

- [`KT-34992`](https://youtrack.jetbrains.com/issue/KT-34992) UI Freeze: Show parameter info leads to freezes

### IDE. Inspections and Intentions

#### New Features

- [`KT-8478`](https://youtrack.jetbrains.com/issue/KT-8478) Make 'Add parameter to function' quick fix work to parameters other than last
- [`KT-12073`](https://youtrack.jetbrains.com/issue/KT-12073) Report IDE inspection warning on pointless unary operators on numbers
- [`KT-18536`](https://youtrack.jetbrains.com/issue/KT-18536) Provide proper quick fix for accidental override error
- [`KT-34218`](https://youtrack.jetbrains.com/issue/KT-34218) Merge 'else if' intention

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
- [`KT-33796`](https://youtrack.jetbrains.com/issue/KT-33796) INVISIBLE_SETTER: quick fix "Make '<set-attribute>' public" does not remove redundant setter
- [`KT-33902`](https://youtrack.jetbrains.com/issue/KT-33902) False positive for "Remove explicit type specification" with type alias as return type
- [`KT-33933`](https://youtrack.jetbrains.com/issue/KT-33933) "Create expect" quick fix generates the declaration in a default source set even if an alternative is chosen
- [`KT-34078`](https://youtrack.jetbrains.com/issue/KT-34078) ReplaceWith does not work if replacement is fun in companion object
- [`KT-34203`](https://youtrack.jetbrains.com/issue/KT-34203) "Add constructor parameter" fix does not add generics
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
- [`KT-35288`](https://youtrack.jetbrains.com/issue/KT-35288) False positive "Remove braces from 'when' entry" in 'when' expression which returns lambda

### IDE. Navigation

- [`KT-30736`](https://youtrack.jetbrains.com/issue/KT-30736) References for import alias from kotlin library not found using ReferencesSearch.search

### IDE. Refactorings

- [`KT-18191`](https://youtrack.jetbrains.com/issue/KT-18191) Refactor / Copy multiple files/classes: package statements are not updated
- [`KT-18539`](https://youtrack.jetbrains.com/issue/KT-18539) Default implement fun/property text shouldn't contain scary comment
- [`KT-28607`](https://youtrack.jetbrains.com/issue/KT-28607) Extract/Introduce variable fails if caret is just after expression
- [`KT-32601`](https://youtrack.jetbrains.com/issue/KT-32601) Introduce variable in unformatted lambda causes PIEAE
- [`KT-34459`](https://youtrack.jetbrains.com/issue/KT-34459) Change method signature with unresolved lambda type leads to error
- [`KT-34971`](https://youtrack.jetbrains.com/issue/KT-34971) Refactor / Copy for declarations from different sources throws IAE: "unexpected element" at CopyFilesOrDirectoriesHandler.getCommonParentDirectory()

### IDE. Run Configurations

- [`KT-34632`](https://youtrack.jetbrains.com/issue/KT-34632) Kotlin/JS: Can not run single test method

### IDE. Script

- [`KT-34688`](https://youtrack.jetbrains.com/issue/KT-34688) Many "scanning dependencies for script definitions progresses at the same time

### IDE. Tests Support

- [`KT-33787`](https://youtrack.jetbrains.com/issue/KT-33787) IDE tests: Not able to run a single test using JUnit

### JS. Tools

- [`KT-35198`](https://youtrack.jetbrains.com/issue/KT-35198) Kotlin/JS: with references to NPM/.kjsm library DCE produces invalid resulting JavaScript

### JavaScript

- [`KT-30517`](https://youtrack.jetbrains.com/issue/KT-30517) KJS generates the wrong call for secondary constructor w/ default argument when class inherited by the object expression
- [`KT-33149`](https://youtrack.jetbrains.com/issue/KT-33149) JS: lambda is not a subtype of `Function<*>`
- [`KT-33327`](https://youtrack.jetbrains.com/issue/KT-33327) JS IR backend works incorrectly when function and property have the same name
- [`KT-33334`](https://youtrack.jetbrains.com/issue/KT-33334) JS IR backend can't access private var from internal inline function

### Libraries

- [`KT-17544`](https://youtrack.jetbrains.com/issue/KT-17544) JS: document array destructuring behavior
- [`KT-33069`](https://youtrack.jetbrains.com/issue/KT-33069) StringBuilder common functions
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

### Tools. Gradle. JS

#### New Features

- [`KT-30659`](https://youtrack.jetbrains.com/issue/KT-30659) Run NodeJS debugger when running debug gradle task from IDEA
- [`KT-32129`](https://youtrack.jetbrains.com/issue/KT-32129) Gradle, JS, Karma: debug
- [`KT-32179`](https://youtrack.jetbrains.com/issue/KT-32179) Gradle, JS, DSL: allow npm in root dependencies section of single platform projects
- [`KT-32283`](https://youtrack.jetbrains.com/issue/KT-32283) Configure webpack mode
- [`KT-32323`](https://youtrack.jetbrains.com/issue/KT-32323) Gradle, JS, webpack: support optimized webpack bundle
- [`KT-32785`](https://youtrack.jetbrains.com/issue/KT-32785) Gradle/JS + Webpack: Asset bundling in distributions folder

#### Fixes

- [`KT-30917`](https://youtrack.jetbrains.com/issue/KT-30917) Gradle, Tests: Inner classes mapped incorrectly in short test fail message
- [`KT-34946`](https://youtrack.jetbrains.com/issue/KT-34946) [Kotlin/JS] DCE require some/all transitive dependencies. Invalid compilation result otherwise
- [`KT-35318`](https://youtrack.jetbrains.com/issue/KT-35318) KJS: IllegalStateException on clean build with `left-pad` package and generateKotlinExternals=true
- [`KT-35428`](https://youtrack.jetbrains.com/issue/KT-35428) [Gradle, JS] Gradle dependency with invalid package.json
- [`KT-35598`](https://youtrack.jetbrains.com/issue/KT-35598) [Gradle, JS] Actualize NPM dependencies in 1.3.70

### Tools. Gradle. Multiplatform

- [`KT-35126`](https://youtrack.jetbrains.com/issue/KT-35126) Support Gradle instant execution for Kotlin/JVM and Android tasks

### Tools. J2K

- [`KT-16774`](https://youtrack.jetbrains.com/issue/KT-16774) UI Freeze: J2K, PlainTextPasteImportResolve: IDEA freezes for 10+ seconds when copy-pasting Java code from an external source to Kotlin file
- [`KT-19574`](https://youtrack.jetbrains.com/issue/KT-19574) Code with inferred default parameters and parameter vs property name clashes
- [`KT-21811`](https://youtrack.jetbrains.com/issue/KT-21811) Convert string concatenation into a multiline string
- [`KT-32551`](https://youtrack.jetbrains.com/issue/KT-32551) New J2K: Non-canonical modifiers order inspection is not applied during conversion of a super inner class
- [`KT-34673`](https://youtrack.jetbrains.com/issue/KT-34673) J2K: first comment in function (if, for, while) block is moved to declaration line of the block
- [`KT-35152`](https://youtrack.jetbrains.com/issue/KT-35152) J2K breaks formatting by moving subsequent single-line comments to the first column

### Tools. Scripts

- [`KT-34274`](https://youtrack.jetbrains.com/issue/KT-34274) Add support for `@CompilerOptions` annotation in `kotlin-main-kts`
- [`KT-34893`](https://youtrack.jetbrains.com/issue/KT-34893) Update apache ivy version in kotlin-main-kts
- [`KT-35413`](https://youtrack.jetbrains.com/issue/KT-35413) Implement "evaluate expression" command line parameter and functionality in the JVM cli compiler
- [`KT-35415`](https://youtrack.jetbrains.com/issue/KT-35415) Implement script and expression evaluation in the `kotlin` runner
- [`KT-35416`](https://youtrack.jetbrains.com/issue/KT-35416) load main-kts script definition by default in the jvm compiler, if the jar is available

### Tools. kapt

- [`KT-30164`](https://youtrack.jetbrains.com/issue/KT-30164) Default field value not transmitted to Java source model for mutable properties
- [`KT-30368`](https://youtrack.jetbrains.com/issue/KT-30368) Deprecated information not transmitted to Java source model
- [`KT-32832`](https://youtrack.jetbrains.com/issue/KT-32832) Kapt: Turn worker API on by default
- [`KT-33617`](https://youtrack.jetbrains.com/issue/KT-33617) KAPT, Java 9+: "IllegalStateException: Should not be called!"
- [`KT-34167`](https://youtrack.jetbrains.com/issue/KT-34167) Kapt: Annotation Processor incorrectly marked as isolating causes full rebuild silently.
- [`KT-34258`](https://youtrack.jetbrains.com/issue/KT-34258) kapt.incremental.apt=true makes build failed after moving annotation processor files
