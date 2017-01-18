# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.1.0-Beta

### Reflection
- [`KT-15540`](https://youtrack.jetbrains.com/issue/KT-15540) findAnnotation returns T?, but it throws NoSuchElementException when there is no matching annotation
- Reflection API in `kotlin-reflect` library is moved to `kotlin.reflect.full` package, declarations in the package `kotlin.reflect` are left deprecated. Please migrate according to the hints provided.

### Compiler

#### Coroutine support
- [`KT-15379`](https://youtrack.jetbrains.com/issue/KT-15379) Allow invoke on instances of suspend function type inside suspend function
- [`KT-15380`](https://youtrack.jetbrains.com/issue/KT-15380) Support suspend function type with value parameters
- [`KT-15391`](https://youtrack.jetbrains.com/issue/KT-15391) Prohibit suspend function type in supertype list
- [`KT-15392`](https://youtrack.jetbrains.com/issue/KT-15392) Prohibit local suspending function
- [`KT-15413`](https://youtrack.jetbrains.com/issue/KT-15413) Override regular functions with suspending ones and vice versa
- [`KT-15657`](https://youtrack.jetbrains.com/issue/KT-15657) Refine dispatchResume convention
- [`KT-15662`](https://youtrack.jetbrains.com/issue/KT-15662) Prohibit callable references to suspend functions

#### Diagnostics
- [`KT-9630`](https://youtrack.jetbrains.com/issue/KT-9630) Cannot create extension function on intersection of types
- [`KT-11398`](https://youtrack.jetbrains.com/issue/KT-11398) Possible false positive for INACCESSIBLE_TYPE
- [`KT-13593`](https://youtrack.jetbrains.com/issue/KT-13593) Do not report USELESS_ELVIS_RIGHT_IS_NULL for left argument with platform type
- [`KT-13859`](https://youtrack.jetbrains.com/issue/KT-13859) Wrong error about using unrepeatable annotation when mix implicit and explicit targets
- [`KT-14179`](https://youtrack.jetbrains.com/issue/KT-14179) Prohibit to use enum entry as type parameter
- [`KT-15097`](https://youtrack.jetbrains.com/issue/KT-15097) Inherited platform declarations clash: regression under 1.1 when indirectly inheriting from java.util.Map
- [`KT-15287`](https://youtrack.jetbrains.com/issue/KT-15287) Kotlin runtime 1.1 and runtime 1.0.x:  Overload resolution ambiguity
- [`KT-15334`](https://youtrack.jetbrains.com/issue/KT-15334) Incorrect "val cannot be reassigned" inside do-while
- [`KT-15410`](https://youtrack.jetbrains.com/issue/KT-15410) "Protected function call from public-API inline function" for protected constructor call

#### Kapt3
- [`KT-15145`](https://youtrack.jetbrains.com/issue/KT-15145) Kapt3: Doesn't compile with multiple errors 
- [`KT-15232`](https://youtrack.jetbrains.com/issue/KT-15232) Kapt3 crash due to java codepage
- [`KT-15359`](https://youtrack.jetbrains.com/issue/KT-15359) Kapt3 exception while annotation processing (DataBindings AS2.3-beta1)
- [`KT-15375`](https://youtrack.jetbrains.com/issue/KT-15375) Kapt3 can't find ${env.JDK_18}/lib/tools.jar
- [`KT-15381`](https://youtrack.jetbrains.com/issue/KT-15381) Unresolved references: R with Kapt3
- [`KT-15397`](https://youtrack.jetbrains.com/issue/KT-15397) Kapt3 doesn't work with databinding
- [`KT-15409`](https://youtrack.jetbrains.com/issue/KT-15409) Kapt3 Cannot find the getter for attribute 'android:text' with value type java.lang.String on android.widget.EditText.
- [`KT-15421`](https://youtrack.jetbrains.com/issue/KT-15421) Kapt3: Substitute types from Psi instead of writing NonExistentClass for generated type names
- [`KT-15459`](https://youtrack.jetbrains.com/issue/KT-15459) Kapt3 doesn't generate code in test module
- [`KT-15524`](https://youtrack.jetbrains.com/issue/KT-15524) Kapt3 - Error messages should display associated element information (if available)
- [`KT-15713`](https://youtrack.jetbrains.com/issue/KT-15713) Kapt3: circular dependencies between Gradke tasks

#### Exceptions / Errors
- [`KT-11401`](https://youtrack.jetbrains.com/issue/KT-11401) Error type encountered for implicit invoke with function literal argument
- [`KT-12044`](https://youtrack.jetbrains.com/issue/KT-12044) Assertion "Rewrite at slice LEXICAL_SCOPE" for 'if' with property references 
- [`KT-14011`](https://youtrack.jetbrains.com/issue/KT-14011) Compiler crash when inlining: lateinit property allRecapturedParameters has not been initialized
- [`KT-14868`](https://youtrack.jetbrains.com/issue/KT-14868) CCE in runtime while converting Number to Char
- [`KT-15364`](https://youtrack.jetbrains.com/issue/KT-15364) VerifyError: Bad type on operand stack on ObserverIterator.hasNext
- [`KT-15373`](https://youtrack.jetbrains.com/issue/KT-15373) Internal error when running TestNG test
- [`KT-15437`](https://youtrack.jetbrains.com/issue/KT-15437) VerifyError: Bad local variable type on simplest provideDelegate
- [`KT-15446`](https://youtrack.jetbrains.com/issue/KT-15446) Property reference on an instance of subclass causes java.lang.VerifyError
- [`KT-15447`](https://youtrack.jetbrains.com/issue/KT-15447) Compiler backend error: "Don't know how to generate outer expression for class"
- [`KT-15449`](https://youtrack.jetbrains.com/issue/KT-15449) Back-end (JVM) Internal error: Couldn't inline method call
- [`KT-15464`](https://youtrack.jetbrains.com/issue/KT-15464) Regression: "Supertypes of the following classes cannot be resolved. Please make sure you have the required dependencies in the classpath:"
- [`KT-15575`](https://youtrack.jetbrains.com/issue/KT-15575) VerifyError: Bad type on operand stack

#### Various issues
- [`KT-11962`](https://youtrack.jetbrains.com/issue/KT-11962) Super call with default parameters check is generated for top-level function
- [`KT-11969`](https://youtrack.jetbrains.com/issue/KT-11969) ProGuard issue with private interface methods
- [`KT-12795`](https://youtrack.jetbrains.com/issue/KT-12795) Write information about sealed class inheritors to metadata
- [`KT-13718`](https://youtrack.jetbrains.com/issue/KT-13718) ClassFormatError on aspectj instrumentation
- [`KT-14162`](https://youtrack.jetbrains.com/issue/KT-14162) Support @InlineOnly on inline properties
- [`KT-14705`](https://youtrack.jetbrains.com/issue/KT-14705) Inconsistent smart casts on when enum subject
- [`KT-14917`](https://youtrack.jetbrains.com/issue/KT-14917) No way to pass additional java command line options to kontlinc on Windows
- [`KT-15112`](https://youtrack.jetbrains.com/issue/KT-15112) Compiler hangs on nested lock compilation
- [`KT-15225`](https://youtrack.jetbrains.com/issue/KT-15225) Scripts: generate classes with names that are valid Java identifiers
- [`KT-15411`](https://youtrack.jetbrains.com/issue/KT-15411) Unnecessary CHECKCAST bytecode when dealing with null
- [`KT-15473`](https://youtrack.jetbrains.com/issue/KT-15473) Invalid KFunction byte code signature for callable references
- [`KT-15582`](https://youtrack.jetbrains.com/issue/KT-15582) Generated bytecode is sometimes incompatible with Java 9
- [`KT-15584`](https://youtrack.jetbrains.com/issue/KT-15584) Do not mark class files compiled with a release language version as pre-release
- [`KT-15589`](https://youtrack.jetbrains.com/issue/KT-15589) Upper bound for T in KClass<T> can be implicitly violated using generic function
- [`KT-15631`](https://youtrack.jetbrains.com/issue/KT-15631) Compiler hang in MethodAnalyzer.analyze() fixed 

### JavaScript backend

#### Coroutine support
- [`KT-15362`](https://youtrack.jetbrains.com/issue/KT-15362) JS: Regex doesn't work (properly) in coroutine
- [`KT-15366`](https://youtrack.jetbrains.com/issue/KT-15366) JS: error when calling inline function with optional parameters from another module inside coroutine lambda
- [`KT-15367`](https://youtrack.jetbrains.com/issue/KT-15367) JS: `for` against iterator with suspend `next` and `hasNext` functions does not work
- [`KT-15400`](https://youtrack.jetbrains.com/issue/KT-15400) suspendCoroutine is missing in JS BE
- [`KT-15597`](https://youtrack.jetbrains.com/issue/KT-15597) Support non-tail suspend calls inside named suspend functions 
- [`KT-15625`](https://youtrack.jetbrains.com/issue/KT-15625) JS: return statement without value surrounded by `try..finally` in suspend lambda causes compiler error
- [`KT-15698`](https://youtrack.jetbrains.com/issue/KT-15698) Move coroutine intrinsics to kotlin.coroutine.intrinsics package

#### Diagnostics
- [`KT-14577`](https://youtrack.jetbrains.com/issue/KT-14577) JS: do not report declaration clash when common redeclaration diagnostic applies
- [`KT-15136`](https://youtrack.jetbrains.com/issue/KT-15136) JS: prohibit inheritance from kotlin Function{N} interfaces

#### Language features support
- [`KT-12194`](https://youtrack.jetbrains.com/issue/KT-12194) Exhaustiveness check isn't generated for when expressions in JS at all
- [`KT-15590`](https://youtrack.jetbrains.com/issue/KT-15590) Support increment on inlined properties
 
#### Native / external
- [`KT-8081`](https://youtrack.jetbrains.com/issue/KT-8081) JS: native inherited class shouldn't require super or primary constructor call
- [`KT-13892`](https://youtrack.jetbrains.com/issue/KT-13892) JS: restrictions for native (external) functions and properties
- [`KT-15307`](https://youtrack.jetbrains.com/issue/KT-15307) JS: prohibit inline members inside external declarations
- [`KT-15308`](https://youtrack.jetbrains.com/issue/KT-15308) JS: prohibit non-abstract members inside external interfaces except nullable properties (with accessors)

#### Exceptions / Errors
- [`KT-7302`](https://youtrack.jetbrains.com/issue/KT-7302) KotlinJS - Trait with optional parameter causes compilation error
- [`KT-15325`](https://youtrack.jetbrains.com/issue/KT-15325) JS: ReferenceError: $receiver is not defined
- [`KT-15357`](https://youtrack.jetbrains.com/issue/KT-15357) JS: `when` expression in primary-from-secondary constructor call
- [`KT-15435`](https://youtrack.jetbrains.com/issue/KT-15435) Call to 'synchronize' crashes JS backend
- [`KT-15513`](https://youtrack.jetbrains.com/issue/KT-15513) JS: empty do..while loop crashes compiler

#### Various issues
- [`KT-4160`](https://youtrack.jetbrains.com/issue/KT-4160) JS: compiler produces wrong code for escaped variable names with characters which Illegal in JS (e.g. spaces)
- [`KT-7004`](https://youtrack.jetbrains.com/issue/KT-7004) JS: functions named `call` not inlined
- [`KT-7588`](https://youtrack.jetbrains.com/issue/KT-7588) JS: operators are not inlined
- [`KT-7733`](https://youtrack.jetbrains.com/issue/KT-7733) JS: Provide overflow behavior for integer arithmetic operations
- [`KT-8413`](https://youtrack.jetbrains.com/issue/KT-8413) JS: generated wrong code for some float constants
- [`KT-12598`](https://youtrack.jetbrains.com/issue/KT-12598) JS: comparisons for Enums always translates using strong operator
- [`KT-13523`](https://youtrack.jetbrains.com/issue/KT-13523) Augmented assignment with array access in LHS is translated incorrectly
- [`KT-13888`](https://youtrack.jetbrains.com/issue/KT-13888) JS: change how functions optional parameters get translated
- [`KT-15260`](https://youtrack.jetbrains.com/issue/KT-15260) JS: don't import module more than once
- [`KT-15475`](https://youtrack.jetbrains.com/issue/KT-15475) JS compiler deletes internal function name in js("") text block
- [`KT-15506`](https://youtrack.jetbrains.com/issue/KT-15506) JS: invalid evaluation order when passing arguments to function by name
- [`KT-15512`](https://youtrack.jetbrains.com/issue/KT-15512) JS: wrong result when use break/throw/return in || and && operators
- [`KT-15569`](https://youtrack.jetbrains.com/issue/KT-15569) js: Wrong code generated when calling an overloaded operator function on an inherited property

### Standard Library
- [`KEEP-23`](https://github.com/Kotlin/KEEP/blob/master/proposals/stdlib/group-and-fold.md) Operation to group by key and fold each group simultaneously
- [`KT-15774`](https://youtrack.jetbrains.com/issue/KT-15774) `buildSequence` and `buildIterator` functions with `yield` and `yieldAll` based on coroutines
- [`KT-6903`](https://youtrack.jetbrains.com/issue/KT-6903) Add `also` extension, which is like `apply`, but with `it` instead of `this` inside lambda.
- [`KT-7858`](https://youtrack.jetbrains.com/issue/KT-7858) Add extension function `takeIf` to match a value against predicate and return null when it does not match
- [`KT-11851`](https://youtrack.jetbrains.com/issue/KT-11851) Provide extension `Map.getValue(key: K): V` which throws or returns default when key is not found
- [`KT-7417`](https://youtrack.jetbrains.com/issue/KT-7417) Add min, max on two numbers to standard library
- [`KT-13898`](https://youtrack.jetbrains.com/issue/KT-13898) Allow to implement `toArray` in collections as protected and provide protected toArray in AbstractCollection.
- [`KT-14935`](https://youtrack.jetbrains.com/issue/KT-14935) Array-like list instantiation functions: `List(count) { init }` and `MutableList(count) { init }` 
- [`KT-15630`](https://youtrack.jetbrains.com/issue/KT-15630) Overloads of mutableListOf, mutableSetOf, mutableMapOf without parameters
- [`KT-15557`](https://youtrack.jetbrains.com/issue/KT-15557) Iterable<T>.joinTo loses information about each element by calling toString on them by default
- [`KT-15477`](https://youtrack.jetbrains.com/issue/KT-15477) Introduce Throwable.addSuppressed extension
- [`KT-15310`](https://youtrack.jetbrains.com/issue/KT-15310) Add dynamic.unsafeCast
- [`KT-15436`](https://youtrack.jetbrains.com/issue/KT-15436) JS stdlib: org.w3c.fetch.RequestInit has 12 parameters, all required
- [`KT-15458`](https://youtrack.jetbrains.com/issue/KT-15458) Add print and println to common stdlib

### IDE
- Project View: Fix presentation of Kotlin files and their members when @JvmName having the same name as the file itself

#### no-arg / all-open
- [`KT-15419`](https://youtrack.jetbrains.com/issue/KT-15419) IDE build doesn't pick settings of all-open plugin
- [`KT-15686`](https://youtrack.jetbrains.com/issue/KT-15686) IDE build doesn't pick settings of no-arg plugin
- [`KT-15735`](https://youtrack.jetbrains.com/issue/KT-15735) Facet loses compiler plugin settings on reopening project, when "Use project settings" = Yes

#### Formatter
- [`KT-15542`](https://youtrack.jetbrains.com/issue/KT-15542) Formatter doesn't handle spaces around 'by' keyword
- [`KT-15544`](https://youtrack.jetbrains.com/issue/KT-15544) Formatter doesn't remove spaces around function reference operator

#### Intention actions, inspections and quick-fixes

##### New features
- Implement quickfix which enables/disables coroutine support in module or project
- [`KT-5045`](https://youtrack.jetbrains.com/issue/KT-5045) Intention to convert between two comparisons and range check and vice versa
- [`KT-5629`](https://youtrack.jetbrains.com/issue/KT-5629) Quick-fix to import extension method when arguments of non-extension method do not match
- [`KT-6217`](https://youtrack.jetbrains.com/issue/KT-6217) Add warning for unused equals expression
- [`KT-6824`](https://youtrack.jetbrains.com/issue/KT-6824) Quick-fix for applying spread operator where vararg is expected
- [`KT-8855`](https://youtrack.jetbrains.com/issue/KT-8855) Implement "Create label" quick-fix
- [`KT-15056`](https://youtrack.jetbrains.com/issue/KT-15056) Implement intention which converts object literal to class
- [`KT-15068`](https://youtrack.jetbrains.com/issue/KT-15068) Implement intention which rename file according to the top-level class name
- [`KT-15564`](https://youtrack.jetbrains.com/issue/KT-15564) Add quick-fix for changing primitive cast to primitive conversion method

##### Bug fixes
- [`KT-14630`](https://youtrack.jetbrains.com/issue/KT-14630) Clearer diagnostic message for platform type inspection
- [`KT-14745`](https://youtrack.jetbrains.com/issue/KT-14745) KNPE in convert primary constructor to secondary
- [`KT-14889`](https://youtrack.jetbrains.com/issue/KT-14889) Replace 'if' with elvis operator produces red code if result is referenced in 'if'
- [`KT-14907`](https://youtrack.jetbrains.com/issue/KT-14907) Quick-fix for missing operator adds infix modifier to created function
- [`KT-15092`](https://youtrack.jetbrains.com/issue/KT-15092) Suppress inspection "use property access syntax" for some getters and fix completion for them
- [`KT-15227`](https://youtrack.jetbrains.com/issue/KT-15227) "Replace if with elvis" silently changes semantics
- [`KT-15412`](https://youtrack.jetbrains.com/issue/KT-15412) "Join declaration and assignment" can break code with smart casts
- [`KT-15501`](https://youtrack.jetbrains.com/issue/KT-15501) Intention "Add names to call arguments" shouldn't appear when the only argument is a trailing lambda

#### Refactorings (Extract / Pull)
- [`KT-15611`](https://youtrack.jetbrains.com/issue/KT-15611) Extract Interface/Superclass: Disable const-properties
- Pull Up: Fix pull-up from object to superclass
- [`KT-15602`](https://youtrack.jetbrains.com/issue/KT-15602) Extract Interface/Superclass: Disable "Make abstract" for inline/external/lateinit members
- Extract Interface: Disable inline/external/lateinit members
- [`KT-12704`](https://youtrack.jetbrains.com/issue/KT-12704), [`KT-15583`](https://youtrack.jetbrains.com/issue/KT-15583) Override/Implement Members: Support all nullability annotations respected by the Kotlin compiler
- [`KT-15563`](https://youtrack.jetbrains.com/issue/KT-15563) Override Members: Allow overriding virtual synthetic members (e.g. equals(), hashCode(), toString(), etc.) in data classes
- [`KT-15355`](https://youtrack.jetbrains.com/issue/KT-15355) Extract Interface: Disable "Make abstract" and assume it to be true for abstract members of an interface
- [`KT-15353`](https://youtrack.jetbrains.com/issue/KT-15353) Extract Superclass/Interface: Allow extracting class with special name (and quotes)
- [`KT-15643`](https://youtrack.jetbrains.com/issue/KT-15643) Extract Interface/Pull Up: Disable "Make abstract" and assume it to be true for primary constructor parameter when moving to an interface
- [`KT-15607`](https://youtrack.jetbrains.com/issue/KT-15607) Extract Interface/Pull Up: Disable internal/protected members when moving to an interface
- [`KT-15640`](https://youtrack.jetbrains.com/issue/KT-15640) Extract Interface/Pull Up: Drop 'final' modifier when moving to an interface
- [`KT-15639`](https://youtrack.jetbrains.com/issue/KT-15639) Extract Superclass/Interface/Pull Up: Add spaces between 'abstract' modifier and annotations
- [`KT-15606`](https://youtrack.jetbrains.com/issue/KT-15606) Extract Interface/Pull Up: Warn about private members with usages in the original class
- [`KT-15635`](https://youtrack.jetbrains.com/issue/KT-15635) Extract Superclass/Interface: Fix bogus visibility warning inside a member when it's being moved as abstract
- [`KT-15598`](https://youtrack.jetbrains.com/issue/KT-15598) Extract Interface: Red-highlight members inherited from a super-interface when that interface reference itself is not extracted
- [`KT-15674`](https://youtrack.jetbrains.com/issue/KT-15674) Extract Superclass: Drop inapplicable modifiers when converting property-parameter to ordinary parameter

#### Multi-platform project support
- [`KT-14908`](https://youtrack.jetbrains.com/issue/KT-14908) Actions (quick-fixes) to create implementations of header elements
- [`KT-15305`](https://youtrack.jetbrains.com/issue/KT-15305) Do not report UNUSED for header declarations with implementations and vice versa
- [`KT-15601`](https://youtrack.jetbrains.com/issue/KT-15601) Cannot suppress HEADER_WITHOUT_IMPLEMENTATION
- [`KT-15641`](https://youtrack.jetbrains.com/issue/KT-15641) Quick-fix "Create header interface implementation" does nothing

#### Android support

- [`KT-12884`](https://youtrack.jetbrains.com/issue/KT-12884) Android Extensions: Refactor / Rename of activity name does not change import extension statement
- [`KT-14308`](https://youtrack.jetbrains.com/issue/KT-14308) Android Studio randomly hangs due to Java static member import quick-fix lags
- [`KT-14358`](https://youtrack.jetbrains.com/issue/KT-14358) Kotlin extensions: rename layout file: Throwable: "PSI and index do not match" through KotlinFullClassNameIndex.get()
- [`KT-15483`](https://youtrack.jetbrains.com/issue/KT-15483) Kotlin lint throws unexpected exceptions in IDE

#### Various issues
- [`KT-12872`](https://youtrack.jetbrains.com/issue/KT-12872) Don't show "defined in <very long qualifier here>" in quick doc for local variables
- [`KT-13001`](https://youtrack.jetbrains.com/issue/KT-13001) "Go to Type Declaration" is broken for stdlib types
- [`KT-13067`](https://youtrack.jetbrains.com/issue/KT-13067) Syntax colouring doesn't work for KDoc tags
- [`KT-14815`](https://youtrack.jetbrains.com/issue/KT-14815) alt + enter -> "import" over a constructor reference is not working
- [`KT-14819`](https://youtrack.jetbrains.com/issue/KT-14819) Quick documentation for special Enum functions doesn't work
- [`KT-15141`](https://youtrack.jetbrains.com/issue/KT-15141) Bogus import popup for when function call cannot be resolved fully
- [`KT-15154`](https://youtrack.jetbrains.com/issue/KT-15154) IllegalStateException on attempt to convert import statement to * if last added import is to typealias
- [`KT-15329`](https://youtrack.jetbrains.com/issue/KT-15329) Regex not inspected properly for javaJavaIdentifierStart and javaJavaIdentifierPart
- [`KT-15383`](https://youtrack.jetbrains.com/issue/KT-15383) Kotlin Scripts can only resolve stdlib functions/classes if they are in a source directory
- [`KT-15440`](https://youtrack.jetbrains.com/issue/KT-15440) Improve extensions detection in IDEA
- [`KT-15548`](https://youtrack.jetbrains.com/issue/KT-15548) Kotlin plugin: @Language injections specified in another module are ignored
- Invoke `StorageComponentContainerContributor` extension for module dependencies container as well (needed for "sam-with-receiver" plugin to work with scripts)

### J2K
- [`KT-6790`](https://youtrack.jetbrains.com/issue/KT-6790) J2K: Static import of Map.Entry is lost during conversion
- [`KT-14736`](https://youtrack.jetbrains.com/issue/KT-14736) J2K: Incorrect conversion of back ticks in javadoc {@code} tag
- [`KT-15027`](https://youtrack.jetbrains.com/issue/KT-15027) J2K: Annotations are set on functions, but not on property accessors

### Gradle support

- [`KT-15376`](https://youtrack.jetbrains.com/issue/KT-15376) Kotlin incremental=true: fixed compatibility with AS 2.3
- [`KT-15433`](https://youtrack.jetbrains.com/issue/KT-15433) Kotlin daemon swallows exceptions: fixed stack trace reporting
- [`KT-15682`](https://youtrack.jetbrains.com/issue/KT-15682) Uncheck "Use project settings" option on import Kotlin project from gradle

### Previous releases

This release also includes the fixes and improvements from the previous
[`1.1-M04`](https://github.com/JetBrains/kotlin/blob/1.1-M04/ChangeLog.md) release.
