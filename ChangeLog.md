# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.1

## 1.0.3

### Compiler

### JS

- Improve performance of maps and sets

#### Analysis & diagnostics

- Combination of 'open' and 'override' is no longer a warning
- [`KT-4829`](https://youtrack.jetbrains.com/issue/KT-4829) Equal conditions in "when" is now a warning
- [`KT-11111`](https://youtrack.jetbrains.com/issue/KT-11111) Overriding by inline function is now a warning, overriding by a function with reified type parameter is an error

###### Issues fixed

- [`KT-4285`](https://youtrack.jetbrains.com/issue/KT-4285) No warning for a non-tail call when the method inherits default arguments from superclass
- [`KT-4764`](https://youtrack.jetbrains.com/issue/KT-4764) Spurious "Variable must be initialized" in try/catch/finally
- [`KT-11750`](https://youtrack.jetbrains.com/issue/KT-11750) Exceptions when creating various entries with the name "name" in enums
- [`KT-11998`](https://youtrack.jetbrains.com/issue/KT-11998) Smart cast to not-null is not performed on a boolean property in 'if' condition

#### JVM code generation

- Optimize coercion to Unit, POP operations are backward-propagated

###### Issues fixed
- [`KT-11499`](https://youtrack.jetbrains.com/issue/KT-11499) Compiler crashes with "Incompatible stack heights"
- [`KT-11943`](https://youtrack.jetbrains.com/issue/KT-11943) CompilationException with extension property of KClass
- [`KT-12125`](https://youtrack.jetbrains.com/issue/KT-12125) Wrong increment/decrement on Byte/Char/Short.MAX_VALUE/MIN_VALUE
- [`KT-12192`](https://youtrack.jetbrains.com/issue/KT-12192) Exhaustiveness check isn't generated for when expression returning Unit

#### Performance

- Reduced number of IO operation when loading kotlin compiled classes

#### Command line compiler

- Support `-language-version` command line option for source compatibility with older releases

### Stdlib

- Improve documentation (including [`KT-11632`](https://youtrack.jetbrains.com/issue/KT-11632))
- List iteration used in collection operations is performed with an indexed loop when the list supports `RandomAccess` and the operation isn't inlined

### IDE

#### Completion

###### New features

- Smart completion after "by" and "in"
- Improved completion in bodies of overridden members (when no type is specified)
- Improved presentation of completion items for property accessors
- Fixed keyword completion after 'try' in assignment expression
- [`KT-8527`](https://youtrack.jetbrains.com/issue/KT-8527) Include non-imported declarations on the first completion
- [`KT-12068`](https://youtrack.jetbrains.com/issue/KT-12068) Special completion item for "[]" get-operator access
- [`KT-12080`](https://youtrack.jetbrains.com/issue/KT-12080) Parameter names are now higher up in completion list

###### Issues fixed
- Fixed enum members being present in completion as static members
- Fixed QuickDoc not working for properties generated for java classes
- [`KT-11609`](https://youtrack.jetbrains.com/issue/KT-11609) No named arguments completion should be after dot
- [`KT-11633`](https://youtrack.jetbrains.com/issue/KT-11633) Wrong indentation after completing a statement in data class
- [`KT-11680`](https://youtrack.jetbrains.com/issue/KT-11680) Code completion of label for existing return with value inserts redundant whitespace
- [`KT-11784`](https://youtrack.jetbrains.com/issue/KT-11784) Completion for 'if' statement should add parentheses automatically
- [`KT-11890`](https://youtrack.jetbrains.com/issue/KT-11890) Completion for callable references does not propose static Java members
- [`KT-11912`](https://youtrack.jetbrains.com/issue/KT-11912) String interpolation is not converted to ${} form when accessing this.property
- [`KT-11957`](https://youtrack.jetbrains.com/issue/KT-11957) No "catch" and "finally" keywords in completion
- [`KT-12103`](https://youtrack.jetbrains.com/issue/KT-12103) Smart completion for nested SAM-adapter produces short unresolved name
- [`KT-12138`](https://youtrack.jetbrains.com/issue/KT-12138) Do not show "::error" in smart completion when any function type accepting one argument is expected
- [`KT-12150`](https://youtrack.jetbrains.com/issue/KT-12150) Smart completion suggests to compare non-nullable with null

#### Spring support

###### New features

- [`KT-11692`](https://youtrack.jetbrains.com/issue/KT-11692) Support Spring model diagrams for Kotlin classes
- [`KT-12079`](https://youtrack.jetbrains.com/issue/KT-12079) Support "Autowired members defined in invalid Spring bean" inspection on Kotlin declarations
- [`KT-12092`](https://youtrack.jetbrains.com/issue/KT-12092) Implement bean references in @Qualifier annotations
- [`KT-12135`](https://youtrack.jetbrains.com/issue/KT-12135) Automatically configure components based on 'basePackageClasses' attribute of @ComponentScan
- [`KT-12136`](https://youtrack.jetbrains.com/issue/KT-12136) Implement package references inside of string literals
- [`KT-12139`](https://youtrack.jetbrains.com/issue/KT-12139) Support Spring configurations linked via @Import annotation
- [`KT-12278`](https://youtrack.jetbrains.com/issue/KT-12278) Implement Spring @Autowired inspection
- [`KT-12465`](https://youtrack.jetbrains.com/issue/KT-12465) Implement Spring @ComponentScan inspection

###### Issues fixed

- [`KT-12091`](https://youtrack.jetbrains.com/issue/KT-12091) Fixed unstable behavior of Spring line markers
- [`KT-12096`](https://youtrack.jetbrains.com/issue/KT-12096) Fixed rename of custom-named beans specified with Kotlin annotation
- [`KT-12117`](https://youtrack.jetbrains.com/issue/KT-12117) Group Kotlin classes from the same file in the Choose Bean dialog
- [`KT-12120`](https://youtrack.jetbrains.com/issue/KT-12120) Show autowiring candidates line markers for @Autowired-annotated constructors and constructor parameters
- [`KT-12122`](https://youtrack.jetbrains.com/issue/KT-12122) Fixed line marker popup on functions with @Qualifier-annotated parameters
- [`KT-12143`](https://youtrack.jetbrains.com/issue/KT-12143) Fixed "Spring Facet Code Configuration (Kotlin)" inspection description
- [`KT-12147`](https://youtrack.jetbrains.com/issue/KT-12147) Fixed exception on analyzing object declaration with @Component annotation
- [`KT-12148`](https://youtrack.jetbrains.com/issue/KT-12148) Warn about object declarations annotated with Spring `@Configuration`/`@Component`/etc.
- [`KT-12363`](https://youtrack.jetbrains.com/issue/KT-12363) Fixed "Autowired members defined in invalid Spring bean (Kotlin)" inspection description
- [`KT-12366`](https://youtrack.jetbrains.com/issue/KT-12366) Fixed exception on analyzing class declaration upon annotation typing
- [`KT-12384`](https://youtrack.jetbrains.com/issue/KT-12384) Fixed bean references in factory method calls

#### Intention actions, inspections and quickfixes

###### New features

- New icon for "New -> Kotlin Activity" action
- "Change visibility on exposure" and "Make visible" fixes now support all possible visibilities
- [`KT-5010`](https://youtrack.jetbrains.com/issue/KT-5010) "Redundant semicolon" inspection with quickfix
- [`KT-9757`](https://youtrack.jetbrains.com/issue/KT-9757) Quickfix for "Unused lambda expression" warning
- [`KT-10844`](https://youtrack.jetbrains.com/issue/KT-10844) Quick fix to add crossinline modifier
- [`KT-11090`](https://youtrack.jetbrains.com/issue/KT-11090) "Add variance modifiers to type parameters" inspection
- [`KT-11255`](https://youtrack.jetbrains.com/issue/KT-11255) Move Element Left/Right actions
- [`KT-11450`](https://youtrack.jetbrains.com/issue/KT-11450) "Modality is redundant" inspection
- [`KT-11523`](https://youtrack.jetbrains.com/issue/KT-11523) "Add @JvmOverloads annotation" intention
- [`KT-11768`](https://youtrack.jetbrains.com/issue/KT-11768) "Introduce local variable" intention
- [`KT-11806`](https://youtrack.jetbrains.com/issue/KT-11806) Quick-fix to increase visibility for invisible member
- [`KT-11807`](https://youtrack.jetbrains.com/issue/KT-11807) Use function body template when generating overriding functions with default body
- [`KT-11876`](https://youtrack.jetbrains.com/issue/KT-11876) Quickfix for "Extension function type is not allowed as supertype" error
- [`KT-11920`](https://youtrack.jetbrains.com/issue/KT-11920) "Increase visibility" and "Decrease visibility" quickfixes for exposed visibility errors
- [`KT-12089`](https://youtrack.jetbrains.com/issue/KT-12089) Quickfix "Make primary constructor parameter a property"
- [`KT-12121`](https://youtrack.jetbrains.com/issue/KT-12121) "Add 'toString()' call" quickfix

###### Issues fixed

- Fixed "Convert property initializer getter" intention being available inside lambda initializer
- Improved message for "Can be declared as 'val'" inspection
- [`KT-11866`](https://youtrack.jetbrains.com/issue/KT-11866) Suggest "Create secondary constructor" when constructors exist but are not applicable
- [`KT-11482`](https://youtrack.jetbrains.com/issue/KT-11482) Fixed exception in "Move to companion object" intention
- [`KT-11483`](https://youtrack.jetbrains.com/issue/KT-11483) Pass implicit receiver as argument when moving member function to companion object
- [`KT-11512`](https://youtrack.jetbrains.com/issue/KT-11512) Allow choosing any source root in "Move file to directory" intention
- [`KT-10950`](https://youtrack.jetbrains.com/issue/KT-10950) Keep original file package name when moving top-level declarations to separate file (provided it's not ambiguous)
- [`KT-10174`](https://youtrack.jetbrains.com/issue/KT-10174) Optimize imports after applying "Move declaration to separate file" intention
- [`KT-11764`](https://youtrack.jetbrains.com/issue/KT-11764) Intention "Replace with a 'forEach' function call should replace 'continue' with 'return@forEach'
- [`KT-11724`](https://youtrack.jetbrains.com/issue/KT-11724) False suggestion to replace with compound assignment
- [`KT-11805`](https://youtrack.jetbrains.com/issue/KT-11805) Invert if-condition intention breaks code in case of end of line comment
- [`KT-11811`](https://youtrack.jetbrains.com/issue/KT-11811) "Make protected" intention for a val declared in parameters of constructor
- [`KT-11710`](https://youtrack.jetbrains.com/issue/KT-11710) "Replace 'if' with elvis operator": incorrect code generated for 'if' expression
- [`KT-11849`](https://youtrack.jetbrains.com/issue/KT-11849) Replace explicit parameter with `it` changes the meaning of code because of the shadowing
- [`KT-11870`](https://youtrack.jetbrains.com/issue/KT-11870) "Replace with Elvis" refactoring doesn't change the variable type from T? to T
- [`KT-12069`](https://youtrack.jetbrains.com/issue/KT-12069) Specify language for all Kotlin code inspections
- [`KT-11366`](https://youtrack.jetbrains.com/issue/KT-11366) "object 'Companion' is never used" warning in intellij
- [`KT-11275`](https://youtrack.jetbrains.com/issue/KT-11275) Inconsistent behaviour of "move lambda argument out of parentheses" intention action when using lambda calls with function arguments without parentheses
- [`KT-11594`](https://youtrack.jetbrains.com/issue/KT-11594) "Add non-null asserted (!!) call" applied to unsafe cast to nullable type causes AE at KtPsiFactory.createExpression()
- [`KT-11982`](https://youtrack.jetbrains.com/issue/KT-11982) False "Redundant final modifier" reported
- [`KT-12040`](https://youtrack.jetbrains.com/issue/KT-12040) "Replace when with if" produce invalid code for first entry which has comment
- [`KT-12204`](https://youtrack.jetbrains.com/issue/KT-12204) 'Use classpath of module' option in existing Kotlin run configuration may be changed when a new run configuration is created
- [`KT-10635`](https://youtrack.jetbrains.com/issue/KT-10635) Don't mark private writeObject and readObject methods of Serializable classes as unused
- [`KT-11466`](https://youtrack.jetbrains.com/issue/KT-11466) "Make abstract" quick fix applies to outer class of object with accidentally abstract function
- [`KT-11120`](https://youtrack.jetbrains.com/issue/KT-11120) Constructor parameter/field reported as unused symbol even if it have 'used' annotation
- [`KT-11974`](https://youtrack.jetbrains.com/issue/KT-11974) Invert if-condition intention loses comments
- [`KT-10812`](https://youtrack.jetbrains.com/issue/KT-10812) Globally unused constructors are not marked as such
- [`KT-11320`](https://youtrack.jetbrains.com/issue/KT-11320) Don't mark @BeforeClass (JUnit4) annotated functions as unused
- [`KT-12267`](https://youtrack.jetbrains.com/issue/KT-12267) "Change type" quick fix converts to Int for Long literal
- [`KT-11949`](https://youtrack.jetbrains.com/issue/KT-11949) Various problems fixed with "Constructor parameter is never used as a property" inspection
- [`KT-11716`](https://youtrack.jetbrains.com/issue/KT-11716) "Simply 'for' using destructuring declaration" intention: incorrect behavior for data classes
- [`KT-12145`](https://youtrack.jetbrains.com/issue/KT-12145) "Simplify 'for' using destructuring declaration" should work even when no variables declared inside loop
- [`KT-11933`](https://youtrack.jetbrains.com/issue/KT-11933) Entities used only by alias are marked as unused

#### Language injection

- Apply injection for the literals in property initializer through property usages
- Enable injection from Java or Kotlin function declaration by annotating parameter with @Language annotation
- [`KT-2428`](https://youtrack.jetbrains.com/issue/KT-2428) Support basic use-cases of language injection for expressions marked with @Language annotation
- [`KT-11574`](https://youtrack.jetbrains.com/issue/KT-11574) Support predefined Java positions for language injection
- [`KT-11472`](https://youtrack.jetbrains.com/issue/KT-11472) Add comment or @Language annotation after "Inject language or reference" intention automatically

#### Refactorings

###### New features
- [`KT-7851`](https://youtrack.jetbrains.com/issue/KT-7851) Respect naming conventions in automatic variable rename
- [`KT-8044`](https://youtrack.jetbrains.com/issue/KT-8044), [`KT-9432`](https://youtrack.jetbrains.com/issue/KT-9432) Support @JvmName annotation in rename refactoring

###### Issues fixed
- [`KT-8860`](https://youtrack.jetbrains.com/issue/KT-8860) Allow renaming class by constructor delegation call referencing primary constructor
- [`KT-8892`](https://youtrack.jetbrains.com/issue/KT-8892) Suggest renaming base declarations on overriding members in object literals
- [`KT-9156`](https://youtrack.jetbrains.com/issue/KT-9156) Quote non-identifier names in Kotlin references
- [`KT-9157`](https://youtrack.jetbrains.com/issue/KT-9157) Fixed in-place rename of Kotlin expression referring Java declaration
- [`KT-9241`](https://youtrack.jetbrains.com/issue/KT-9241) Do not replace Java references to synthetic component functions when renaming constructor parameter
- [`KT-9444`](https://youtrack.jetbrains.com/issue/KT-9444) Rename dialog: Allow typing any identifier without backquotes
- [`KT-10713`](https://youtrack.jetbrains.com/issue/KT-10713) Skip read-only declarations when renaming parameters

#### Java to Kotlin converter

###### New features

- [`KT-4727`](https://youtrack.jetbrains.com/issue/KT-4727) Convert Java code copied from browser or other sources

###### Issues fixed

- [`KT-11952`](https://youtrack.jetbrains.com/issue/KT-11952) Assertion failed in PropertyDetectionCache.get on conversion of access to Java constant of anonymous type
- [`KT-12046`](https://youtrack.jetbrains.com/issue/KT-12046) Recursive property setter
- [`KT-12039`](https://youtrack.jetbrains.com/issue/KT-12039) Static imports converted missing ".Companion"
- [`KT-12054`](https://youtrack.jetbrains.com/issue/KT-12054) Wrong conversion of 'instanceof' checks with raw types
- [`KT-12045`](https://youtrack.jetbrains.com/issue/KT-12045) Convert `Object()` to `Any()`

#### KDoc

###### New features:
- Support for @receiver tag

###### Issues fixed:
- Rendering of '_' and '*' standalone characters
- Rendering of code blocks
- [`KT-9933`](https://youtrack.jetbrains.com/issue/KT-9933) Indentation in code fragments is not preserved
- [`KT-10998`](https://youtrack.jetbrains.com/issue/KT-10998) Spaces around links are missing in return block
- [`KT-11791`](https://youtrack.jetbrains.com/issue/KT-11791) Markdown links rendering
- [`KT-12001`](https://youtrack.jetbrains.com/issue/KT-12001) Allow use of '@param' to document type parameter

#### Maven support

###### New features:
- Inspections that check that kotlin IDEA plugin, kotlin Maven plugin and kotlin stdlib are of the same version
- [`KT-11643`](https://youtrack.jetbrains.com/issue/KT-11643) Inspections and intentions to fix erroneously configured Maven pom file
- [`KT-11701`](https://youtrack.jetbrains.com/issue/KT-11701) "Add Maven Dependency quick fix" in Kotlin source files
- [`KT-11743`](https://youtrack.jetbrains.com/issue/KT-11743) Intention to replace kotlin-test with kotlin-test-junit

###### Issues fixed:
- [`KT-9492`](https://youtrack.jetbrains.com/issue/KT-9492) Configuring multiple Maven Modules
- [`KT-11642`](https://youtrack.jetbrains.com/issue/KT-11642) Kotlin Maven configurator tags order
- [`KT-11436`](https://youtrack.jetbrains.com/issue/KT-11436) "Choose Configurator" control opens dialogs with inconsistent modality (linux)
- [`KT-11731`](https://youtrack.jetbrains.com/issue/KT-11731) Default maven integration doesn't include documentation

#### Debugger

###### New features:
- [`KT-11438`](https://youtrack.jetbrains.com/issue/KT-11438) Support navigation from stacktrace to inline function call site

###### Issues fixed:
- Do not step into inline lambda argument during step over inside inline function body
- Fix step over for inline argument with non-local return
- [`KT-12067`](https://youtrack.jetbrains.com/issue/KT-12067) Deadlock in Kotlin debugger is fixed
- [`KT-12232`](https://youtrack.jetbrains.com/issue/KT-12232) No code completion in Evaluate Expression and Throwable at CodeCompletionHandlerBase.invokeCompletion()
- [`KT-12137`](https://youtrack.jetbrains.com/issue/KT-12137) Evaluate expression: code completion/intention actions allows to use symbols from modules that are not referenced

#### Other

###### New features

- Added "Decompile" button to Kotlin bytecode toolwindow
- [`KT-11657`](https://youtrack.jetbrains.com/issue/KT-11657) Allow viewing decompiled Java source code for Kotlin-compiled classes
- [`KT-11704`](https://youtrack.jetbrains.com/issue/KT-11704) Support file path references inside of Kotlin string literals

###### Issues fixed:

- [`KT-8817`](https://youtrack.jetbrains.com/issue/KT-8817) Fixed rename of Java getters/setters through synthetic property references in Kotlin
- [`KT-11145`](https://youtrack.jetbrains.com/issue/KT-11145) Use progress indicator when searching usages in Introduce Parameter
- [`KT-11155`](https://youtrack.jetbrains.com/issue/KT-11155) Allow running multiple Kotlin classes as well as running mixtures of Kotlin and Java classes
- [`KT-11495`](https://youtrack.jetbrains.com/issue/KT-11495) Show recursion line markers for extension function calls with different receiver
- [`KT-11659`](https://youtrack.jetbrains.com/issue/KT-11659) Generate abstract overrides for Any members inside of Kotlin interfaces
- [`KT-12035`](https://youtrack.jetbrains.com/issue/KT-12035) Auto-format cast expressions
- [`KT-12018`](https://youtrack.jetbrains.com/issue/KT-12018) Auto-format spaces between function name and arguments in infix calls
- [`KT-12070`](https://youtrack.jetbrains.com/issue/KT-12070) Add empty line in error message of Maven and Gradle configuration
- [`KT-11961`](https://youtrack.jetbrains.com/issue/KT-11961) Formatter doesn't handle space before angle bracket in method definition
- [`KT-12175`](https://youtrack.jetbrains.com/issue/KT-12175) Formatter: Don't enforce empty line between secondary constructors without body
- [`KT-11908`](https://youtrack.jetbrains.com/issue/KT-11908) Allow properties with custom setters to be used in generated equals/hashCode/toString
- [`KT-11617`](https://youtrack.jetbrains.com/issue/KT-11617) Fixed title of Introduce Parameter declaration chooser
- [`KT-11817`](https://youtrack.jetbrains.com/issue/KT-11817) Fixed rename of Kotlin enum constants through Java references
- [`KT-11816`](https://youtrack.jetbrains.com/issue/KT-11816) Fixed usages search for Safe Delete on simple enum entries
- [`KT-11282`](https://youtrack.jetbrains.com/issue/KT-11282) Delete interface reference from super-type list when applying Safe Delete to Java interface
- [`KT-11967`](https://youtrack.jetbrains.com/issue/KT-11967) Fix Find Usages/Rename for parameter references in XML files
- [`KT-10770`](https://youtrack.jetbrains.com/issue/KT-10770) "Optimize imports" will not keep import if a type is only referenced by kdoc
- [`KT-11955`](https://youtrack.jetbrains.com/issue/KT-11955) Copy/Paste inserts fully qualified name when copying function with overloads
- [`KT-12206`](https://youtrack.jetbrains.com/issue/KT-12206) Fix NoSuchFieldError on accessing base property without backing field in evaluate expression


### Reflection

###### Issues fixed:
- [`KT-11531`](https://youtrack.jetbrains.com/issue/KT-11531) Optimize "KCallable.name"

### JS

###### Issues fixed:
- [`KT-7228`](https://youtrack.jetbrains.com/issue/KT-7228) Wrong AbstractList signature
- [`KT-8299`](https://youtrack.jetbrains.com/issue/KT-8299) Wrong access to private member in autogenerated code in data class
- [`KT-11346`](https://youtrack.jetbrains.com/issue/KT-11346) Reified functions like `filterIsInstance` are now available in JS Standard Library
- [`KT-12305`](https://youtrack.jetbrains.com/issue/KT-12305) Incorrect translation of `vararg` in `@native` functions
- [`KT-12254`](https://youtrack.jetbrains.com/issue/KT-12254) JsEmptyExpression in initializer when compiling code like `val x = throw Exception()`

### Tools

###### New features

- [`KT-11839`](https://youtrack.jetbrains.com/issue/KT-11839) Maven goal to execute kotlin script

###### Issues fixed

- [`KT-12074`](https://youtrack.jetbrains.com/issue/KT-12074) Building Kotlin maven projects using a parent pom will silently fail

## 1.0.2

### Compiler

#### Analysis & diagnostics

- [KT-7437](https://youtrack.jetbrains.com/issue/KT-7437), [KT-7971](https://youtrack.jetbrains.com/issue/KT-7971), [KT-7051](https://youtrack.jetbrains.com/issue/KT-7051), [KT-6125](https://youtrack.jetbrains.com/issue/KT-6125), [KT-6186](https://youtrack.jetbrains.com/issue/KT-6186), [KT-11649](https://youtrack.jetbrains.com/issue/KT-11649) Implement missing checks for protected visibility
- [KT-11666](https://youtrack.jetbrains.com/issue/KT-11666) Report "Implicit nothing return type" on non-override member functions
- [KT-4328](https://youtrack.jetbrains.com/issue/KT-4328), [KT-11497](https://youtrack.jetbrains.com/issue/KT-11497), [KT-10493](https://youtrack.jetbrains.com/issue/KT-10493), [KT-10820](https://youtrack.jetbrains.com/issue/KT-10820), [KT-11368](https://youtrack.jetbrains.com/issue/KT-11368) Report error if some classes were not found due to missing or conflicting dependencies
- [KT-11280](https://youtrack.jetbrains.com/issue/KT-11280) Do not perform smart casts for values with custom `equals` compared with `==`
- [KT-3856](https://youtrack.jetbrains.com/issue/KT-3856) Fix wrong "inner class inaccessible" diagnostic for extension to outer class
- [KT-3896](https://youtrack.jetbrains.com/issue/KT-3896), [KT-3883](https://youtrack.jetbrains.com/issue/KT-3883), [KT-4986](https://youtrack.jetbrains.com/issue/KT-4986) `do...while (true)` is now considered an infinite loop
- [KT-10445](https://youtrack.jetbrains.com/issue/KT-10445) Prohibit initialization of captured `val` in lambda or in local function
- [KT-10042](https://youtrack.jetbrains.com/issue/KT-10042) Correctly handle local classes and anonymous objects in control flow analysis
- [KT-11043](https://youtrack.jetbrains.com/issue/KT-11043) Prohibit complex expressions with class literals in annotation arguments
- [KT-10992](https://youtrack.jetbrains.com/issue/KT-10992), [KT-11007](https://youtrack.jetbrains.com/issue/KT-11007) Fix multiple problems related to smart casts
- [KT-11490](https://youtrack.jetbrains.com/issue/KT-11490) Prohibit nested intersection types in return position
- [KT-11411](https://youtrack.jetbrains.com/issue/KT-11411) Report "illegal noinline/crossinline" on parameter of subtype of function type
- [KT-3083](https://youtrack.jetbrains.com/issue/KT-3083) Report "conflicting overloads" for functions with parameter of type parameter type
- [KT-7265](https://youtrack.jetbrains.com/issue/KT-7265) Parse anonymous functions in blocks as expressions
- [KT-8246](https://youtrack.jetbrains.com/issue/KT-8246) Handle break/continue for outer loop correctly in case of try/finally in between
- [KT-11300](https://youtrack.jetbrains.com/issue/KT-11300) Report error on increment or augmented assignment when `get` is an operator but `set` is not
- Report warning about unused anonymous functions
- Improve callable reference type in some ambiguous cases
- Improve multiple diagnostic messages: [KT-10761](https://youtrack.jetbrains.com/issue/KT-10761), [KT-9760](https://youtrack.jetbrains.com/issue/KT-9760), [KT-10949](https://youtrack.jetbrains.com/issue/KT-10949), [KT-9887](https://youtrack.jetbrains.com/issue/KT-9887), [KT-9550](https://youtrack.jetbrains.com/issue/KT-9550), [KT-11239](https://youtrack.jetbrains.com/issue/KT-11239), [KT-11819](https://youtrack.jetbrains.com/issue/KT-11819)
- Fix several compiler bugs leading to exceptions: [KT-9820](https://youtrack.jetbrains.com/issue/KT-9820), [KT-11597](https://youtrack.jetbrains.com/issue/KT-11597), [KT-10983](https://youtrack.jetbrains.com/issue/KT-10983), [KT-10972](https://youtrack.jetbrains.com/issue/KT-10972), [KT-11287](https://youtrack.jetbrains.com/issue/KT-11287), [KT-11492](https://youtrack.jetbrains.com/issue/KT-11492), [KT-11765](https://youtrack.jetbrains.com/issue/KT-11765), [KT-11869](https://youtrack.jetbrains.com/issue/KT-11869)

#### JVM code generation

- [KT-8269](https://youtrack.jetbrains.com/issue/KT-8269), [KT-9246](https://youtrack.jetbrains.com/issue/KT-9246), [KT-10143](https://youtrack.jetbrains.com/issue/KT-10143) Fix visibility of protected classes in bytecode
- [KT-11363](https://youtrack.jetbrains.com/issue/KT-11363) Fix potential binary compatibility breakage on using `when` over enums in inline functions
- [KT-11762](https://youtrack.jetbrains.com/issue/KT-11762) Fix VerifyError caused by explicit loop variable type
- [KT-11645](https://youtrack.jetbrains.com/issue/KT-11645) Fix NoSuchFieldError on private const property in multi-file class
- [KT-9670](https://youtrack.jetbrains.com/issue/KT-9670) Optimize Class <-> KClass wrapping/unwrapping when getting values from annotation
- [KT-6842](https://youtrack.jetbrains.com/issue/KT-6842) Optimize unnecessary boxing and interface calls on iterating over ranges
- [KT-11025](https://youtrack.jetbrains.com/issue/KT-11025) Don't inline const val properties in non-annotation contexts
- [KT-5429](https://youtrack.jetbrains.com/issue/KT-5429) Write nullability annotations on extension receiver parameters
- [KT-11347](https://youtrack.jetbrains.com/issue/KT-11347) Preserve source file and line number of call site when inlining certain standard library functions
- [KT-11677](https://youtrack.jetbrains.com/issue/KT-11677) Write correct generic signatures for local classes in inlined lambdas
- [KT-12127](https://youtrack.jetbrains.com/issue/KT-12127) Do not write unnecessary generic signature for property delegate backing field
- Fix multiple issues leading to exceptions or bad bytecode being generated: [KT-11034](https://youtrack.jetbrains.com/issue/KT-11034), [KT-11519](https://youtrack.jetbrains.com/issue/KT-11519), [KT-11117](https://youtrack.jetbrains.com/issue/KT-11117), [KT-11479](https://youtrack.jetbrains.com/issue/KT-11479)

#### Java interoperability

- [KT-3068](https://youtrack.jetbrains.com/issue/KT-3068) Load contravariantly projected collections in Java (`List<? super T>`) as mutable collections in Kotlin (`MutableList<in T>`)
- [KT-11322](https://youtrack.jetbrains.com/issue/KT-11322) Do not lose type nullability information in SAM constructors
- [KT-11721](https://youtrack.jetbrains.com/issue/KT-11721) Fix wrong "Typechecker has run into recursive problem" error on calling Kotlin get function as synthetic Java property
- [KT-10691](https://youtrack.jetbrains.com/issue/KT-10691) Fix wrong "Inherited platform declarations clash" error on inheritance from generic Java class with overloaded methods

#### Command line compiler

- [KT-9546](https://youtrack.jetbrains.com/issue/KT-9546) Flush stdout and stderr before shutdown when executing scripts
- [KT-10605](https://youtrack.jetbrains.com/issue/KT-10605) Disable colored output on certain platforms to prevent crashes
- Report warning instead of error on unknown "-X" flags
- Remove the compiler option "Xmultifile-facades-open"

#### Compiler daemon

- Reduce read disk activity
- Fix compiler daemon JAR cache clearing on IDEA Ultimate

### Standard library

- [KT-11410](https://youtrack.jetbrains.com/issue/KT-11410) Reduce method count of the standard library by ~2k
- [KT-9990](https://youtrack.jetbrains.com/issue/KT-9990) Optimize snapshot operations to return special collection implementations when result is empty or has single element
- [KT-10794](https://youtrack.jetbrains.com/issue/KT-10794) EmptyList now implements RandomAccess
- [KT-10821](https://youtrack.jetbrains.com/issue/KT-10821) Create at most one wrapper sequence for adjacent drop/take operations on sequences
- [KT-11301](https://youtrack.jetbrains.com/issue/KT-11301) Make Map.plus accept Map out-projected by key type as either operand (receiver or parameter)
- [KT-11485](https://youtrack.jetbrains.com/issue/KT-11485) Remove implementations of some internal intrinsic functions
- [KT-11648](https://youtrack.jetbrains.com/issue/KT-11648) Add deprecated extension MutableList.remove to redirect to valid function removeAt
- [KT-11348](https://youtrack.jetbrains.com/issue/KT-11348) kotlin.test: Make inline methods `todo` and `currentStackTrace` `@InlineOnly` not to lose stack trace
- [KT-11745](https://youtrack.jetbrains.com/issue/KT-11745) Rename parameters of `String.subSequence` to match those of `CharSequence.subSequence`
- [KT-10953](https://youtrack.jetbrains.com/issue/KT-10953) Clarify parameter order of lambda function parameter of `*Indexed` functions
- [KT-10198](https://youtrack.jetbrains.com/issue/KT-10198) Improve docs for `binarySearch` functions
- [KT-9786](https://youtrack.jetbrains.com/issue/KT-9786) Improve docs for `trimIndent`/`trimMargin`

### Reflection

- [KT-9952](https://youtrack.jetbrains.com/issue/KT-9952) Improve `toString()` for lambdas and function expressions when kotlin-reflect.jar is available
- [KT-11433](https://youtrack.jetbrains.com/issue/KT-11433) Fix multiple resource leaks by closing InputStream instances
- [KT-8131](https://youtrack.jetbrains.com/issue/KT-8131) Fix exception from calling `KProperty.javaField` on a subclass
- [KT-10690](https://youtrack.jetbrains.com/issue/KT-10690) Support `javaMethod` and `kotlinFunction` for top level functions in a different file
- [KT-11447](https://youtrack.jetbrains.com/issue/KT-11447) Support reflection calls to multifile class members
- [KT-10892](https://youtrack.jetbrains.com/issue/KT-10892) Load annotations of const properties from multifile classes
- [KT-11258](https://youtrack.jetbrains.com/issue/KT-11258) Don't crash on requesting members of Java collection classes
- [KT-11502](https://youtrack.jetbrains.com/issue/KT-11502) Clarify KClass equality

### JS

- [KT-4124](https://youtrack.jetbrains.com/issue/KT-4124) Support nested classes
- [KT-11030](https://youtrack.jetbrains.com/issue/KT-11030) Support local classes
- [KT-7819](https://youtrack.jetbrains.com/issue/KT-7819) Support non-local returns in local lambdas
- [KT-6912](https://youtrack.jetbrains.com/issue/KT-6912) Safe calls (`x?.let { it }`) are now inlined
- [KT-2670](https://youtrack.jetbrains.com/issue/KT-2670) Support unsafe casts (`as`)
- [KT-7016](https://youtrack.jetbrains.com/issue/KT-7016), [KT-8012](https://youtrack.jetbrains.com/issue/KT-8012) Fix `is`-checks for reified type parameters
- [KT-7038](https://youtrack.jetbrains.com/issue/KT-7038) Avoid unwanted side effects on `is`-checks for nullable types
- [KT-10614](https://youtrack.jetbrains.com/issue/KT-10614) Copy array on vararg call with spread operator
- [KT-10785](https://youtrack.jetbrains.com/issue/KT-10785) Correctly translate property names and receiver instances in assignment operations
- [KT-11611](https://youtrack.jetbrains.com/issue/KT-11611) Fix translation of default value of secondary constructor's functional parameter
- [KT-11100](https://youtrack.jetbrains.com/issue/KT-11100) Fix generation of `invoke` on objects and companion objects
- [KT-11823](https://youtrack.jetbrains.com/issue/KT-11823) Fix capturing of outer class' `this` in inner's lambdas
- [KT-11996](https://youtrack.jetbrains.com/issue/KT-11996) Fix translation of a call to a private member of an outer class from an inner class which is a subtype of the outer class
- [KT-10667](https://youtrack.jetbrains.com/issue/KT-10667) Support inheritance from nested built-in types such as Map.Entry
- [KT-7480](https://youtrack.jetbrains.com/issue/KT-7480) Remove declarations of LinkedList, SortedSet, TreeSet, Enumeration
- [KT-3064](https://youtrack.jetbrains.com/issue/KT-3064) Implement `CharSequence.repeat`

### IDE

New features:

- Spring Support
  - [KT-11098](https://youtrack.jetbrains.com/issue/KT-11098) Inspection on final classes/functions annotated with Spring `@Configuration`/`@Component`/`@Bean`
  - [KT-11405](https://youtrack.jetbrains.com/issue/KT-11405) Navigation and Find Usages for Spring beans referenced in annotation arguments and BeanFactory method calls
  - [KT-3741](https://youtrack.jetbrains.com/issue/KT-3741) Show Spring-specific line markers on Kotlin classes
  - [KT-11406](https://youtrack.jetbrains.com/issue/KT-11406) Support Spring EL injections inside of Kotlin string literals
  - [KT-11604](https://youtrack.jetbrains.com/issue/KT-11604) Support "Configure Spring facet" inspection on Kotlin classes
  - [KT-11407](https://youtrack.jetbrains.com/issue/KT-11407) Implement "Generate Spring Dependency..." actions
  - [KT-11408](https://youtrack.jetbrains.com/issue/KT-11408) Implement "Generate `@Autowired` Dependency..." action
  - [KT-11652](https://youtrack.jetbrains.com/issue/KT-11652) Rename bean attributes mentioned in Spring XML config together with corresponding Kotlin declarations
- Enable precise incremental compilation by default in non-Maven/Gradle projects
- [KT-11612](https://youtrack.jetbrains.com/issue/KT-11612) Highlight named arguments
- [KT-7715](https://youtrack.jetbrains.com/issue/KT-7715) Highlight `var`s that can be replaced by `val`s
- [KT-5208](https://youtrack.jetbrains.com/issue/KT-5208) Intention action to convert string to raw string and back
- [KT-11078](https://youtrack.jetbrains.com/issue/KT-11078) Quick fix to remove `.java` when KClass is expected
- [KT-1494](https://youtrack.jetbrains.com/issue/KT-1494) Inspection to highlight public members with no documentation
- [KT-8473](https://youtrack.jetbrains.com/issue/KT-8473) Intention action to implement interface or abstract class
- [KT-10299](https://youtrack.jetbrains.com/issue/KT-10299) Inspection to warn on array properties in data classes
- [KT-6674](https://youtrack.jetbrains.com/issue/KT-6674) Inspection to warn on protected symbols in effectively final classes
- [KT-11576](https://youtrack.jetbrains.com/issue/KT-11576) Quick fix to suppress "Unused symbol" warning based on annotations on the declaration
- [KT-10063](https://youtrack.jetbrains.com/issue/KT-10063) Quick fix for adding `arrayOf` wrapper for annotation parameters
- [KT-10476](https://youtrack.jetbrains.com/issue/KT-10476) Quick fix for converting primitive types
- [KT-10859](https://youtrack.jetbrains.com/issue/KT-10859) Quick fix to make `var` with private setter final
- [KT-9498](https://youtrack.jetbrains.com/issue/KT-9498) Quick fix to specify property type
- [KT-10509](https://youtrack.jetbrains.com/issue/KT-10509) Quick fix to simplify condition with senseless comparison
- [KT-11404](https://youtrack.jetbrains.com/issue/KT-11404) Quick fix to let type implement missing interface
- [KT-6785](https://youtrack.jetbrains.com/issue/KT-6785), [KT-10013](https://youtrack.jetbrains.com/issue/KT-10013), [KT-9996](https://youtrack.jetbrains.com/issue/KT-9996), [KT-11675](https://youtrack.jetbrains.com/issue/KT-11675) Support Smart Enter for trailing lambda argument, try/catch/finally, property setter, init block
- Add `kotlinClassName()` and `kotlinFunctionName()` macros for use in live templates
- Auto-configure EAP-repository during Kotlin Maven and Gradle project set up

Issues fixed:

- [KT-11678](https://youtrack.jetbrains.com/issue/KT-11678), [KT-4768](https://youtrack.jetbrains.com/issue/KT-4768) Support navigation to Kotlin libraries from Java sources
- [KT-9401](https://youtrack.jetbrains.com/issue/KT-9401) Support Change Signature quick fix for Java -> Kotlin case
- [KT-8592](https://youtrack.jetbrains.com/issue/KT-8592) Fix "Choose sources" for Kotlin files
- [KT-11256](https://youtrack.jetbrains.com/issue/KT-11256) Fix Navigate to declaration for Java constructor with `@NotNull` parameter
- [KT-11018](https://youtrack.jetbrains.com/issue/KT-11018) Fix `var`s shown in Ctrl + Mouse Hover as `val`s
- [KT-5105](https://youtrack.jetbrains.com/issue/KT-5105), [KT-11024](https://youtrack.jetbrains.com/issue/KT-11024) Improve incompatible ABI versions editor strap, show the hint on how to resolve the problem
- [KT-11638](https://youtrack.jetbrains.com/issue/KT-11638) Fixed `hashCode()` implementation in "Generate equals/hashCode" action
- [KT-10971](https://youtrack.jetbrains.com/issue/KT-10971) Pull Members Up: Always insert spaces between keywords
- [KT-11476](https://youtrack.jetbrains.com/issue/KT-11476), [KT-4175](https://youtrack.jetbrains.com/issue/KT-4175), [KT-10965](https://youtrack.jetbrains.com/issue/KT-10965), [KT-11076](https://youtrack.jetbrains.com/issue/KT-11076) Formatter: fix multiple issues regarding space handling
- [KT-9025](https://youtrack.jetbrains.com/issue/KT-9025) Improve "Create Kotlin Java runtime library" dialog usability
- [KT-11481](https://youtrack.jetbrains.com/issue/KT-11481) Fix "Add import" intention not being available for `is` branches in when
- [KT-10619](https://youtrack.jetbrains.com/issue/KT-10619) Fix completion after package name in annotation
- [KT-10621](https://youtrack.jetbrains.com/issue/KT-10621) Do not show non-top level packages after `@` in completion
- [KT-11295](https://youtrack.jetbrains.com/issue/KT-11295) "Convert string to template" intention: fix exception on certain code
- [KT-10750](https://youtrack.jetbrains.com/issue/KT-10750), [KT-11424](https://youtrack.jetbrains.com/issue/KT-11424) "Convert if to when" intention now detects effectively else branches in subsequent code and performs more accurate comment handling
- Configure Kotlin: show only changed files in the notification "Kotlin not configured", restore all changed files in undo action
- [KT-11556](https://youtrack.jetbrains.com/issue/KT-11556) Do not show "Kotlin not configured" for Kotlin JS projects
- [KT-11593](https://youtrack.jetbrains.com/issue/KT-11593) Fix "Configure Kotlin" action for Gradle projects in IDEA 2016
- [KT-11077](https://youtrack.jetbrains.com/issue/KT-11077) Use new built-in definition file format (`.kotlin_builtins` files)
- [KT-5728](https://youtrack.jetbrains.com/issue/KT-5728) Remove closing curly brace in a string template when opening one is deleted
- [KT-10883](https://youtrack.jetbrains.com/issue/KT-10883) "Explicit get or set call" quick fix: do not move caret too far away
- [KT-5717](https://youtrack.jetbrains.com/issue/KT-5717) "Replace 'when' with 'if'": do not lose comments
- [KT-10797](https://youtrack.jetbrains.com/issue/KT-10797) "Replace with operator" intention is not available anymore for non-`operator` functions
- [KT-11529](https://youtrack.jetbrains.com/issue/KT-11529) Highlighting range for unresolved annotation name does not include `@` now
- [KT-11178](https://youtrack.jetbrains.com/issue/KT-11178) Don't show "Change type arguments" fix when there's nothing to change
- [KT-11789](https://youtrack.jetbrains.com/issue/KT-11789) Don't interpret annotations inside Markdown code blocks as KDoc tags
- [KT-11702](https://youtrack.jetbrains.com/issue/KT-11702) Fixed resolution of Kotlin beans with custom name
- [KT-11689](https://youtrack.jetbrains.com/issue/KT-11689) Fixed exception on attempt to navigate to Kotlin file from Spring notification balloon
- [KT-11725](https://youtrack.jetbrains.com/issue/KT-11725) Fixed renaming of injected SpEL references
- [KT-11720](https://youtrack.jetbrains.com/issue/KT-11720) Fixed renaming of Kotlin beans through SpEL references
- [KT-11719](https://youtrack.jetbrains.com/issue/KT-11719) Fixed renaming of Kotlin parameters references in XML files
- [KT-11736](https://youtrack.jetbrains.com/issue/KT-11736) Fixed searching of Java usages for @JvmStatic properties and @JvmStatic @JvmOverloads functions
- [KT-11862](https://youtrack.jetbrains.com/issue/KT-11862) Fixed bogus warnings about unresolved types in the Change Signature dialog
- Fix several issues leading to exceptions: [KT-11579](https://youtrack.jetbrains.com/issue/KT-11579), [KT-11580](https://youtrack.jetbrains.com/issue/KT-11580), [KT-11777](https://youtrack.jetbrains.com/issue/KT-11777), [KT-11868](https://youtrack.jetbrains.com/issue/KT-11868), [KT-11845](https://youtrack.jetbrains.com/issue/KT-11845), [KT-11486](https://youtrack.jetbrains.com/issue/KT-11486)
- Fixed NoSuchFieldException in Kotlin module settings on IDEA Ultimate

#### Debugger

- [KT-11705](https://youtrack.jetbrains.com/issue/KT-11705) "Smart step into" no longer skips methods from subclasses
- Debugger can now distinguish nested inline arguments
- [KT-11326](https://youtrack.jetbrains.com/issue/KT-11326) Support private classes in Evaluate Expression
- [KT-11455](https://youtrack.jetbrains.com/issue/KT-11455) Fix Evaluate Expression behavior for files with errors in sources
- [KT-10670](https://youtrack.jetbrains.com/issue/KT-10670) Fix Evaluate Expression behavior for inline functions with default parameters
- [KT-11380](https://youtrack.jetbrains.com/issue/KT-11380) Evaluate Expression now handles smart casts correctly
- [KT-10148](https://youtrack.jetbrains.com/issue/KT-10148) Do not suggest methods from outer context in "Smart step into"
- Fix Evaluate Expression for expression created for array element
- Complete private members from libraries in Evaluate Expression
- [KT-11578](https://youtrack.jetbrains.com/issue/KT-11578) Evaluate Expression: do not highlight completion variants from nullable receiver with grey
- [KT-6805](https://youtrack.jetbrains.com/issue/KT-6805) Convert Java expression to Kotlin when opening Evaluate Expression from Variables view
- [KT-11927](https://youtrack.jetbrains.com/issue/KT-11927) Fix "ambiguous import" error when invoking Evaluate Expression from Variables view for some field
- [KT-11831](https://youtrack.jetbrains.com/issue/KT-11831) Fix Evaluate Expression for values of raw types
- Show error message when debug info for some local variable is corrupted
- Avoid 1s delay in completion in debugger fields if session is not stopped on a breakpoint
- Avoid cast to runtime type unavailable in current scope
- Fix text with line breaks in popup with line breakpoint variants
- Fix breakpoints inside inline functions in libraries sources
- Allow breakpoints at catch clause declaration
- [KT-11848](https://youtrack.jetbrains.com/issue/KT-11848) Fix breakpoints inside generic crossinline lambda argument body
- [KT-11932](https://youtrack.jetbrains.com/issue/KT-11932) Fix Step Over for `while` loop condition

### Java to Kotlin converter

- Protected members used outside of inheritors are converted as public
- Support conversion for annotation constructor calls
- Place comments from the middle of the call to the end
- Drop line breaks between operator arguments (except `+`, `-`, `&&` and `||`)
- Add non-null assertions on call site for non-null parameters
- Specify type for variables with anonymous type if they have write accesses
- [KT-11587](https://youtrack.jetbrains.com/issue/KT-11587) Fix conversion of static field accesses from other Java class
- [KT-6800](https://youtrack.jetbrains.com/issue/KT-6800) Quote `$` symbols in converted strings
- [KT-11126](https://youtrack.jetbrains.com/issue/KT-11126) Convert annotations in annotations parameters correctly
- [KT-11600](https://youtrack.jetbrains.com/issue/KT-11600) Do not produce unresolved `toArray` calls for Java `Collection#toArray(T[])`
- [KT-11544](https://youtrack.jetbrains.com/issue/KT-11544) Fix conversion of uninitialized non-final field
- [KT-10604](https://youtrack.jetbrains.com/issue/KT-10604) Fix conversion of scratch files
- [KT-11543](https://youtrack.jetbrains.com/issue/KT-11543) Do not produce unnecessary casts of non-nullable expression to nullable type
- [KT-11160](https://youtrack.jetbrains.com/issue/KT-11160) Fix IDE freeze

### Android

- [KT-7729](https://youtrack.jetbrains.com/issue/KT-7729) Add Android Lint checks for Kotlin (from Android Studio 1.5)
- [KT-11487](https://youtrack.jetbrains.com/issue/KT-11487) Fixed sequential build with kapt and stubs enabled when Kotlin source file was modified and no Java source files were modified
- [KT-11264](https://youtrack.jetbrains.com/issue/KT-11264) Action to create new activity in Kotlin
- [KT-11201](https://youtrack.jetbrains.com/issue/KT-11201) Do not ignore items with similar names in kapt
- [KT-11944](https://youtrack.jetbrains.com/issue/KT-11944) Rename Android Extensions imports when the layout file is renamed/deleted/added
- [KT-10321](https://youtrack.jetbrains.com/issue/KT-10321) Do not upcast ViewStub to View
- [KT-10841](https://youtrack.jetbrains.com/issue/KT-10841) Support `@android:id/*` IDs in Android Extensions

### Maven

- [KT-2917](https://youtrack.jetbrains.com/issue/KT-2917), [KT-11261](https://youtrack.jetbrains.com/issue/KT-11261) Maven archetype for new Kotlin projects

### Gradle

- [KT-8487](https://youtrack.jetbrains.com/issue/KT-8487) Experimental support for incremental compilation with project property `kotlin.incremental`
- [KT-11350](https://youtrack.jetbrains.com/issue/KT-11350) Fixed a bug causing Java rebuild when both Java and Kotlin are up-to-date
- [KT-10507](https://youtrack.jetbrains.com/issue/KT-10507) Fix IllegalArgumentException "Missing extension point" on parallel builds
- [KT-10932](https://youtrack.jetbrains.com/issue/KT-10932) Prevent compile tasks from running when nothing changes
- [KT-11993](https://youtrack.jetbrains.com/issue/KT-11993) Fix NoSuchMethodError on access to internal members in production from tests (IDEA 2016+)

## 1.0.1-2

### Compiler

- [KT-11584](https://youtrack.jetbrains.com/issue/KT-11584), [KT-11514](https://youtrack.jetbrains.com/issue/KT-11514) Correct comparison of Long! / Double! with integer constant
- [KT-11590](https://youtrack.jetbrains.com/issue/KT-11590) SAM adapter for inline function corrected

## 1.0.1-1

### Compiler

- [KT-11468](https://youtrack.jetbrains.com/issue/KT-11468) More correct use-site / declaration-site variance combination handling
- [KT-11478](https://youtrack.jetbrains.com/issue/KT-11478) "Couldn't inline method call" internal compiler error fixed

## 1.0.1

### Compiler

Analysis & diagnostics issues fixed:

- [KT-2277](https://youtrack.jetbrains.com/issue/KT-2277) Local function declarations are now checked for overload conflicts
- [KT-3602](https://youtrack.jetbrains.com/issue/KT-3602)  Special diagnostic is reported now on nullable ‘for’ range
- [KT-10775](https://youtrack.jetbrains.com/issue/KT-10775) No compilation exception for empty when
- [KT-10952](https://youtrack.jetbrains.com/issue/KT-10952) False deprecation warnings removed
- [KT-10934](https://youtrack.jetbrains.com/issue/KT-10934) Type inference improved for whens
- [KT-10902](https://youtrack.jetbrains.com/issue/KT-10902) Redeclaration is reported for top-level property vs classifier conflict
- [KT-9985](https://youtrack.jetbrains.com/issue/KT-9985)  Correct handling of safe call arguments in generic functions
- [KT-10856](https://youtrack.jetbrains.com/issue/KT-10856) Diagnostic about projected out member is reported correctly on calls with smart cast receiver
- [KT-5190](https://youtrack.jetbrains.com/issue/KT-5190)  Calls of Java 8 Stream.collect
- [KT-11109](https://youtrack.jetbrains.com/issue/KT-11109) Warning is reported on Strictfp annotation on a class because it's not supported yet
- [KT-10686](https://youtrack.jetbrains.com/issue/KT-10686) Support generic constructors defined in Java
- [KT-6958](https://youtrack.jetbrains.com/issue/KT-6958)  Fixed resolution for overloaded functions with extension lambdas
- [KT-10765](https://youtrack.jetbrains.com/issue/KT-10765) Correct handling of overload conflict between constructor and function in JPS
- [KT-10752](https://youtrack.jetbrains.com/issue/KT-10752) If inferred type for an expression refers to a non-accessible Java class, it's a compiler error to prevent IAE in runtime
- [KT-7415](https://youtrack.jetbrains.com/issue/KT-7415) Approximation of captured types in signatures
- [KT-10913](https://youtrack.jetbrains.com/issue/KT-10913), [KT-10186](https://youtrack.jetbrains.com/issue/KT-10186), [KT-5198](https://youtrack.jetbrains.com/issue/KT-5198) False “unreachable code” fixed for various situations
- Minor: [KT-3680](https://youtrack.jetbrains.com/issue/KT-3680), [KT-9702](https://youtrack.jetbrains.com/issue/KT-9702), [KT-8776](https://youtrack.jetbrains.com/issue/KT-8776), [KT-6745](https://youtrack.jetbrains.com/issue/KT-6745), [KT-10919](https://youtrack.jetbrains.com/issue/KT-10919), [KT-9548](https://youtrack.jetbrains.com/issue/KT-9548)

JVM code generation issues fixed:

- [KT-11153](https://youtrack.jetbrains.com/issue/KT-11153) NoClassDefFoundError is fixed on primitive iterators during boxing optimization
- [KT-7319](https://youtrack.jetbrains.com/issue/KT-7319)  Correct parameter names for @JvmOverloads-generated methods
- [KT-10425](https://youtrack.jetbrains.com/issue/KT-10425) Non-const values of member properties are not inlined now
- [KT-11163](https://youtrack.jetbrains.com/issue/KT-11163) Correct calls of custom compareTo on primitives
- [KT-11081](https://youtrack.jetbrains.com/issue/KT-11081) Reified type parameters are correctly stored in anonymous objects
- [KT-11121](https://youtrack.jetbrains.com/issue/KT-11121) Generic properties generation is fixed for interfaces
- [KT-11285](https://youtrack.jetbrains.com/issue/KT-11285), [KT-10958](https://youtrack.jetbrains.com/issue/KT-10958) Special bridge generation refined
- [KT-10313](https://youtrack.jetbrains.com/issue/KT-10313), [KT-11190](https://youtrack.jetbrains.com/issue/KT-11190), [KT-11192](https://youtrack.jetbrains.com/issue/KT-11192), [KT-11130](https://youtrack.jetbrains.com/issue/KT-11130) Diagnostics and bytecode fixed for various operations with Long
- [KT-11203](https://youtrack.jetbrains.com/issue/KT-11203), [KT-11191](https://youtrack.jetbrains.com/issue/KT-11191), [KT-11206](https://youtrack.jetbrains.com/issue/KT-11206), [KT-8505](https://youtrack.jetbrains.com/issue/KT-8505), [KT-11203](https://youtrack.jetbrains.com/issue/KT-11203) Handling of increment / decrement for collection elements with user-defined get / set fixed
- [KT-9739](https://youtrack.jetbrains.com/issue/KT-9739)  Backticked names with spaces are generated correctly

JS translator issues fixed:

- [KT-7683](https://youtrack.jetbrains.com/issue/KT-7683), [KT-11027](https://youtrack.jetbrains.com/issue/KT-11027) correct handling of in / !in inside when expressions

### Standard library

- [KT-10579](https://youtrack.jetbrains.com/issue/KT-10579) Improved performance of sum() and average() for arrays
- [KT-10821](https://youtrack.jetbrains.com/issue/KT-10821) Improved performance of drop() / take() for sequences

### Reflection

- [KT-10840](https://youtrack.jetbrains.com/issue/KT-10840) Fix annotations on Java elements in reflection

### IDE

New features:

- Compatibility with IDEA 2016
- Kotlin Education Plugin (for IDEA 2016)
- [KT-9752](https://youtrack.jetbrains.com/issue/KT-9752)  More usable file chooser for "Move declaration to another file"
- [KT-9697](https://youtrack.jetbrains.com/issue/KT-9697)  Move method to companion object and back
- [KT-7443](https://youtrack.jetbrains.com/issue/KT-7443) Inspection + intention to replace assert (x != null) with "!!" or elvis

General issues fixed:

- [KT-11277](https://youtrack.jetbrains.com/issue/KT-11277) Correct moving of Java classes from project view
- [KT-11256](https://youtrack.jetbrains.com/issue/KT-11256) Navigate Declaration fixed for Java classes with @NotNull parameter in constructor
- [KT-10553](https://youtrack.jetbrains.com/issue/KT-10553) A warning provided when Refactor / Move result is not compilable due to visibility problems
- [KT-11039](https://youtrack.jetbrains.com/issue/KT-11039) Parameter names are now not missing in parameter info and completion for compiled java code used from kotlin
- [KT-10204](https://youtrack.jetbrains.com/issue/KT-10204) Highlight usages in file is working now for function parameter
- [KT-10954](https://youtrack.jetbrains.com/issue/KT-10954) Introduce Parameter (Ctrl+Alt+P) fixed when default value is a simple name reference
- [KT-10776](https://youtrack.jetbrains.com/issue/KT-10776) Intentions: "Convert to lambda expression" works now for empty function body
- [KT-10815](https://youtrack.jetbrains.com/issue/KT-10815) Generate equals() and hashCode() is no more suggested for interfaces
- [KT-10818](https://youtrack.jetbrains.com/issue/KT-10818) "Initialize with constructor parameter" fixed
- [KT-8876](https://youtrack.jetbrains.com/issue/KT-8876) "Convert member to extension" now removes modality modifiers (open / final)
- [KT-10800](https://youtrack.jetbrains.com/issue/KT-10800) Create enum entry now adds comma after a new entry
- [KT-10552](https://youtrack.jetbrains.com/issue/KT-10552) Pull Members Up now takes visibility conflicts into account
- [KT-10978](https://youtrack.jetbrains.com/issue/KT-10978) Partially fixed, completion for JOOQ became ~ 10 times faster
- [KT-10940](https://youtrack.jetbrains.com/issue/KT-10940) Reference search optimized for convention functions
- [KT-9026](https://youtrack.jetbrains.com/issue/KT-9026)  Editor no more locks up during scala file viewing
- [KT-11142](https://youtrack.jetbrains.com/issue/KT-11142), [KT-11276](https://youtrack.jetbrains.com/issue/KT-11276) Darkula scheme appearance corrected for Kotlin
- Minor: [KT-10778](https://youtrack.jetbrains.com/issue/KT-10778), [KT-10763](https://youtrack.jetbrains.com/issue/KT-10763), [KT-10908](https://youtrack.jetbrains.com/issue/KT-10908), [KT-10345](https://youtrack.jetbrains.com/issue/KT-10345), [KT-10696](https://youtrack.jetbrains.com/issue/KT-10696), [KT-11041](https://youtrack.jetbrains.com/issue/KT-11041), [KT-9434](https://youtrack.jetbrains.com/issue/KT-9434), [KT-8744](https://youtrack.jetbrains.com/issue/KT-8744), [KT-9738](https://youtrack.jetbrains.com/issue/KT-9738), [KT-10912](https://youtrack.jetbrains.com/issue/KT-10912)

Configuration issues fixed:

- [KT-11213](https://youtrack.jetbrains.com/issue/KT-11213) Kotlin plugin version corrected in build.gradle
- [KT-10918](https://youtrack.jetbrains.com/issue/KT-10918) "Update Kotlin runtime" action does not try to update the runtime coming in from Gradle
- [KT-11072](https://youtrack.jetbrains.com/issue/KT-11072) Libraries in maven, gradle and ide systems are never more detected as runtime libraries
- [KT-10489](https://youtrack.jetbrains.com/issue/KT-10489) Configuration messages are aggregated into one notification
- [KT-10831](https://youtrack.jetbrains.com/issue/KT-10831) Configure Kotlin in Project: "All modules containing Kotlin files" does not list modules not containing Kotlin files
- [KT-10366](https://youtrack.jetbrains.com/issue/KT-10366) Gradle import: no fake "Configure Kotlin" notification on project creating

Debugger issues fixed:

- [KT-10827](https://youtrack.jetbrains.com/issue/KT-10827) Fixed debugger stepping for inline calls
- [KT-10780](https://youtrack.jetbrains.com/issue/KT-10780) Breakpoints in a lazy property work correctly
- [KT-10634](https://youtrack.jetbrains.com/issue/KT-10634) Watches can now use private overloaded functions
- [KT-10611](https://youtrack.jetbrains.com/issue/KT-10611) Line breakpoints now can be created inside lambda in init block
- [KT-10673](https://youtrack.jetbrains.com/issue/KT-10673) Breakpoints inside lambda are no more ignored in presence of crossinline function parameter
- [KT-11318](https://youtrack.jetbrains.com/issue/KT-11318) Stepping inside for each is optimized
- [KT-3873](https://youtrack.jetbrains.com/issue/KT-3873)  Editing code while standing on breakpoint is optimized
- [KT-7261](https://youtrack.jetbrains.com/issue/KT-7261), [KT-7266](https://youtrack.jetbrains.com/issue/KT-7266), [KT-10672](https://youtrack.jetbrains.com/issue/KT-10672) Evaluate expression applicability corrected

### Tools

- [KT-7943](https://youtrack.jetbrains.com/issue/KT-7943), [KT-10127](https://youtrack.jetbrains.com/issue/KT-10127) Overhead removed in Kotlin Gradle Plugin
- [KT-11351](https://youtrack.jetbrains.com/issue/KT-11351) Fixed NoSuchMethodError with Gradle 2.12
