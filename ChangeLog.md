# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.1.3

### Compiler
- [`KT-4960`](https://youtrack.jetbrains.com/issue/KT-4960) Redeclaration is not reported for type parameters of interfaces
- [`KT-5160`](https://youtrack.jetbrains.com/issue/KT-5160) No warning when a lambda parameter hides a variable
- [`KT-5246`](https://youtrack.jetbrains.com/issue/KT-5246) is check fails on cyclic type parameter bounds
- [`KT-5354`](https://youtrack.jetbrains.com/issue/KT-5354) Wrong label resolution when label name clash with fun name
- [`KT-7645`](https://youtrack.jetbrains.com/issue/KT-7645) Prohibit default value for `catch`-block parameter
- [`KT-7724`](https://youtrack.jetbrains.com/issue/KT-7724) Can never import private member 
- [`KT-7796`](https://youtrack.jetbrains.com/issue/KT-7796) Wrong scope for default parameter value resolution
- [`KT-7931`](https://youtrack.jetbrains.com/issue/KT-7931) Optimize iteration over strings/charsequences on JVM
- [`KT-7984`](https://youtrack.jetbrains.com/issue/KT-7984) Unexpected "Unresolved reference" in a default value expression in a local function
- [`KT-7985`](https://youtrack.jetbrains.com/issue/KT-7985) Unexpected "Unresolved reference to a type parameter" in a default value expression in a local function
- [`KT-8320`](https://youtrack.jetbrains.com/issue/KT-8320) It should not be possible to catch a type parameter type
- [`KT-8877`](https://youtrack.jetbrains.com/issue/KT-8877) Automatic labeling doesn't work for infix calls
- [`KT-9251`](https://youtrack.jetbrains.com/issue/KT-9251) Qualified this does not work with labeled function literals
- [`KT-9370`](https://youtrack.jetbrains.com/issue/KT-9370) not possible to pass an argument that starts with "-" to a script using kotlinc
- [`KT-9551`](https://youtrack.jetbrains.com/issue/KT-9551) False warning "No cast needed"
- [`KT-9645`](https://youtrack.jetbrains.com/issue/KT-9645) Incorrect inspection: No cast Needed
- [`KT-9986`](https://youtrack.jetbrains.com/issue/KT-9986) 'null as T' should be unchecked cast
- [`KT-10397`](https://youtrack.jetbrains.com/issue/KT-10397) java.lang.reflect.GenericSignatureFormatError when generic inner class is mentioned in function signature
- [`KT-10848`](https://youtrack.jetbrains.com/issue/KT-10848) Optimize substitution of inline function with default parameters
- [`KT-11167`](https://youtrack.jetbrains.com/issue/KT-11167) Support compilation against JRE 9
- [`KT-11622`](https://youtrack.jetbrains.com/issue/KT-11622) False "No cast needed" when ambiguous call because of smart cast
- [`KT-12049`](https://youtrack.jetbrains.com/issue/KT-12049) Kotlin Lint: "Missing Parcelable CREATOR field" could suggest "Add implementation" quick fix
- [`KT-12245`](https://youtrack.jetbrains.com/issue/KT-12245) Code with annotation that has an unresolved identifier as a parameter compiles successfully
- [`KT-12269`](https://youtrack.jetbrains.com/issue/KT-12269) False "Non-null type is checked for instance of nullable type"
- [`KT-12497`](https://youtrack.jetbrains.com/issue/KT-12497) Optimize inlined bytecode for functions with default parameters
- [`KT-12683`](https://youtrack.jetbrains.com/issue/KT-12683) A problem with `is` operator and non-reified type-parameters
- [`KT-12690`](https://youtrack.jetbrains.com/issue/KT-12690) USELESS_CAST compiler warning may break code when fix is applied
- [`KT-13348`](https://youtrack.jetbrains.com/issue/KT-13348) Report useless cast on safe cast from nullable type to the same not null type
- [`KT-13597`](https://youtrack.jetbrains.com/issue/KT-13597) No check for accessing final field in local object in constructor
- [`KT-14381`](https://youtrack.jetbrains.com/issue/KT-14381) Possible val reassignment not detected inside init block
- [`KT-14564`](https://youtrack.jetbrains.com/issue/KT-14564) java.lang.VerifyError: Bad local variable type
- [`KT-14977`](https://youtrack.jetbrains.com/issue/KT-14977) IDE doesn't warn about checking null value of variable that cannot be null
- [`KT-15050`](https://youtrack.jetbrains.com/issue/KT-15050) Random build failures using maven 3 (multi-thread) + bamboo
- [`KT-15085`](https://youtrack.jetbrains.com/issue/KT-15085) Label and function naming conflict is resolved in unintuitive way
- [`KT-15161`](https://youtrack.jetbrains.com/issue/KT-15161) False warning "no cast needed" for array creation
- [`KT-15495`](https://youtrack.jetbrains.com/issue/KT-15495) Internal typealiases in the same module are inaccessible on incremental compilation
- [`KT-15566`](https://youtrack.jetbrains.com/issue/KT-15566) Object member imported in file scope used in delegation expression in object declaration should be a compiler error
- [`KT-16283`](https://youtrack.jetbrains.com/issue/KT-16283) Maven compiler plugin warns, "Source root doesn't exist"
- [`KT-16426`](https://youtrack.jetbrains.com/issue/KT-16426) Return statement resolved to function instead of property getter
- [`KT-16712`](https://youtrack.jetbrains.com/issue/KT-16712) Show warning in IDEA when using Java 1.8 api in Android
- [`KT-16743`](https://youtrack.jetbrains.com/issue/KT-16743) Update configuration options in Kotlin Maven plugin
- [`KT-16754`](https://youtrack.jetbrains.com/issue/KT-16754) J2K: Apply quick-fixes from EDT thread only
- [`KT-16762`](https://youtrack.jetbrains.com/issue/KT-16762) Maven: JS compiler option main is missing
- [`KT-16813`](https://youtrack.jetbrains.com/issue/KT-16813) Anonymous objects returned from private-in-file members should behave as for private class members
- [`KT-16816`](https://youtrack.jetbrains.com/issue/KT-16816) Java To Kotlin bug: if + chained assignment doesn't include brackets
- [`KT-16843`](https://youtrack.jetbrains.com/issue/KT-16843) Android: provide gutter icons for resources like colors and drawables
- [`KT-16986`](https://youtrack.jetbrains.com/issue/KT-16986) header symbols referring types from standard library are not matched with their implementations in IDE
- [`KT-17093`](https://youtrack.jetbrains.com/issue/KT-17093) Import from maven: please provide a special tag for coroutine option
- [`KT-17100`](https://youtrack.jetbrains.com/issue/KT-17100) "kotlin" launcher script: do not add current working directory to classpath if explicit "-classpath" is specified
- [`KT-17112`](https://youtrack.jetbrains.com/issue/KT-17112) IncompatibleClassChangeError on invoking Kotlin compiler daemon on JDK 9
- [`KT-17140`](https://youtrack.jetbrains.com/issue/KT-17140) Warning "classpath entry points to a file that is not a jar file" could just be disabled
- [`KT-17144`](https://youtrack.jetbrains.com/issue/KT-17144) Breakpoint inside `when`
- [`KT-17149`](https://youtrack.jetbrains.com/issue/KT-17149) Incorrect warning "Kotlin: This operation has led to an overflow"
- [`KT-17245`](https://youtrack.jetbrains.com/issue/KT-17245) Kapt: Javac compiler arguments can't be specified in Gradle
- [`KT-17264`](https://youtrack.jetbrains.com/issue/KT-17264) Change the format of advanced CLI arguments ("-X...") to require value after "=", not a whitespace
- [`KT-17287`](https://youtrack.jetbrains.com/issue/KT-17287) Configure kotlin in Android Studio: don't show menu Choose Configurator with single choice
- [`KT-17288`](https://youtrack.jetbrains.com/issue/KT-17288) Android Studio: please remove menu item Configure Kotlin (JavaScript) in Project
- [`KT-17289`](https://youtrack.jetbrains.com/issue/KT-17289) Android Studio: Hide "Configure Kotlin" balloon if Kotlin is configured from a kt file banner
- [`KT-17291`](https://youtrack.jetbrains.com/issue/KT-17291) Android Studio 2.4: Cannot get property 'metaClass' on null object error after Kotlin configuring
- [`KT-17318`](https://youtrack.jetbrains.com/issue/KT-17318) Typo in DSL Marker message `cant`
- [`KT-17342`](https://youtrack.jetbrains.com/issue/KT-17342) Optimize control-flow for case of many variables
- [`KT-17384`](https://youtrack.jetbrains.com/issue/KT-17384) break/continue expression in inlined function parameter argument causes compilation exception
- [`KT-17387`](https://youtrack.jetbrains.com/issue/KT-17387) When compiling in the IDE, progress tracker says "configuring the compilation environment" when it clearly isn't
- [`KT-17389`](https://youtrack.jetbrains.com/issue/KT-17389) Implement Intention "Add Activity / BroadcastReceiver / Service to manifest" 
- [`KT-17418`](https://youtrack.jetbrains.com/issue/KT-17418) "The following options were not recognized by any processor: '[kapt.kotlin.generated]'" warning from Javac shouldn't be shown even if no processor supports the generated annotation
- [`KT-17448`](https://youtrack.jetbrains.com/issue/KT-17448) Regression: Sample Resolve 
- [`KT-17456`](https://youtrack.jetbrains.com/issue/KT-17456) kapt3: NoClassDefFound com/sun/tools/javac/util/Context
- [`KT-17465`](https://youtrack.jetbrains.com/issue/KT-17465) Add intentions Add/Remove/Redo parcelable implementation
- [`KT-17479`](https://youtrack.jetbrains.com/issue/KT-17479) val reassign is allowed via explicit this receiver
- [`KT-17560`](https://youtrack.jetbrains.com/issue/KT-17560) Overload resolution ambiguity on semi-valid class-files generated by Scala
- [`KT-17562`](https://youtrack.jetbrains.com/issue/KT-17562) Optimize KtFile::isScript
- [`KT-17572`](https://youtrack.jetbrains.com/issue/KT-17572) try-catch expression in inlined function parameter argument causes compilation exception
- [`KT-17573`](https://youtrack.jetbrains.com/issue/KT-17573) try-finally expression in inlined function parameter argument fails with VerifyError
- [`KT-17588`](https://youtrack.jetbrains.com/issue/KT-17588) Compiler error while optimizer tries to get rid of captured variable
- [`KT-17590`](https://youtrack.jetbrains.com/issue/KT-17590) conditional return in inline function parameter argument causes compilation exception
- [`KT-17591`](https://youtrack.jetbrains.com/issue/KT-17591) non-conditional return in inline function parameter argument causes compilation exception

### IDE
- [`KT-7810`](https://youtrack.jetbrains.com/issue/KT-7810) Separate icon for abstract class
- [`KT-8370`](https://youtrack.jetbrains.com/issue/KT-8370) "Can't move to original file" should not be an error
- [`KT-8930`](https://youtrack.jetbrains.com/issue/KT-8930) Refactor / Move preivew: moved element is shown as reference, and its file as subject
- [`KT-9158`](https://youtrack.jetbrains.com/issue/KT-9158) Refactor / Move preview mentions the package statement of moved class as a usage
- [`KT-10577`](https://youtrack.jetbrains.com/issue/KT-10577) Refactor / Move Kotlin + Java files adds wrong import in very specific case
- [`KT-10981`](https://youtrack.jetbrains.com/issue/KT-10981) Quickfix for INAPPLICABLE_JVM_FIELD to replace with 'const' when possible
- [`KT-11250`](https://youtrack.jetbrains.com/issue/KT-11250) Auto-completion for convention function names in 'operator fun' definitions
- [`KT-12293`](https://youtrack.jetbrains.com/issue/KT-12293) Autocompletion should propose `lateinit var` in addition to `lateinit`
- [`KT-12629`](https://youtrack.jetbrains.com/issue/KT-12629) Add rainbow/semantic-highlighting for local variables
- [`KT-13192`](https://youtrack.jetbrains.com/issue/KT-13192) Refactor / Move: to another class: "To" field code completion suggests facade and Java classes
- [`KT-13466`](https://youtrack.jetbrains.com/issue/KT-13466) Refactor / Move: class to upper level: the package statement is not updated
- [`KT-13524`](https://youtrack.jetbrains.com/issue/KT-13524) Completing the keyword 'constructor' before a primary constructor wrongly inserts parentheses
- [`KT-13673`](https://youtrack.jetbrains.com/issue/KT-13673) Add 'companion { ... }'  code completion opsion
- [`KT-14046`](https://youtrack.jetbrains.com/issue/KT-14046) Add intention to add inline keyword if a function has parameter with noinline and/or crossinline modifier
- [`KT-14109`](https://youtrack.jetbrains.com/issue/KT-14109) support parameter hints in idea plugin
- [`KT-14435`](https://youtrack.jetbrains.com/issue/KT-14435) "Use destructuring declaration" should be available as intention even without usages
- [`KT-14601`](https://youtrack.jetbrains.com/issue/KT-14601) Formatter inserts unnecessary indent before 'else'
- [`KT-14665`](https://youtrack.jetbrains.com/issue/KT-14665) No completion for "else" keyword
- [`KT-14974`](https://youtrack.jetbrains.com/issue/KT-14974) "Find Usages" hangs in ExpressionsOfTypeProcessor
- [`KT-15273`](https://youtrack.jetbrains.com/issue/KT-15273) Kotlin IDE plugin adds `import java.lang.String` with "Optimize Imports", making project broken
- [`KT-15519`](https://youtrack.jetbrains.com/issue/KT-15519) KDoc comments for data class values get removed by Change Signature
- [`KT-15543`](https://youtrack.jetbrains.com/issue/KT-15543) "Convert receiver to parameter" refactoring breaks code
- [`KT-15603`](https://youtrack.jetbrains.com/issue/KT-15603) Annoying completion when making a primary constructor private
- [`KT-15660`](https://youtrack.jetbrains.com/issue/KT-15660) Quick-fix "Create header interface implementation" chooses wrong source root
- [`KT-15680`](https://youtrack.jetbrains.com/issue/KT-15680) Implementations gutter icon for header interface shows duplicates
- [`KT-15854`](https://youtrack.jetbrains.com/issue/KT-15854) Debugger not able to evaluate internal member functions
- [`KT-15903`](https://youtrack.jetbrains.com/issue/KT-15903) QuickFix to add/remove suspend in hierarchies
- [`KT-16025`](https://youtrack.jetbrains.com/issue/KT-16025) Step into suspend functions stops at the function end
- [`KT-16136`](https://youtrack.jetbrains.com/issue/KT-16136) Wrong type parameter variance suggested if type parameter is used in nested anonymous object
- [`KT-16161`](https://youtrack.jetbrains.com/issue/KT-16161) Completion of 'onEach' inserts unneeded angular brackets
- [`KT-16339`](https://youtrack.jetbrains.com/issue/KT-16339) Incorrect warning: 'protected' visibility is effectively 'private' in a final class
- [`KT-16392`](https://youtrack.jetbrains.com/issue/KT-16392) Gradle/Maven java module: Add framework support/ Kotlin (Java or JavaScript) adds nothing
- [`KT-16645`](https://youtrack.jetbrains.com/issue/KT-16645) Support inlay type hints for implicitly typed vals, properties, and functions
- [`KT-16775`](https://youtrack.jetbrains.com/issue/KT-16775) Rewrite at slice CLASS key: OBJECT_DECLARATION while writing code in IDE
- [`KT-16786`](https://youtrack.jetbrains.com/issue/KT-16786) Intention to add "open" modifier to a non-private method or property in an open class
- [`KT-16838`](https://youtrack.jetbrains.com/issue/KT-16838) Navigate from header to impl shows all overloads
- [`KT-16850`](https://youtrack.jetbrains.com/issue/KT-16850) UI freeze for several seconds during inserting selected completion variant
- [`KT-17037`](https://youtrack.jetbrains.com/issue/KT-17037) Editor suggests to import `EmptyCoroutineContext.plus` for any unresolved `+`
- [`KT-17046`](https://youtrack.jetbrains.com/issue/KT-17046) Kotlin facet, Compiler plugins: last line is shown empty when not selected
- [`KT-17053`](https://youtrack.jetbrains.com/issue/KT-17053) Inspection to detect use of callable reference as a lambda body
- [`KT-17088`](https://youtrack.jetbrains.com/issue/KT-17088) Settings: Kotlin Compiler: "Destination directory" should be enabled if "Copy library runtime files" is on on the dialog opening 
- [`KT-17094`](https://youtrack.jetbrains.com/issue/KT-17094) Kotlin facet, additional command line parameters dialog: please provide a title
- [`KT-17138`](https://youtrack.jetbrains.com/issue/KT-17138) Configure Kotlin in Project: Choose Configurator popup: names could be unified
- [`KT-17145`](https://youtrack.jetbrains.com/issue/KT-17145) Kotlin facet: IllegalArgumentException on attempt to show settings common for several javascript kotlin facets with different moduleKind
- [`KT-17191`](https://youtrack.jetbrains.com/issue/KT-17191) Intention to name anonymous (_) parameter
- [`KT-17211`](https://youtrack.jetbrains.com/issue/KT-17211) Refactor / Move several files: superfluous FQN is inserted into reference to same file's element
- [`KT-17213`](https://youtrack.jetbrains.com/issue/KT-17213) Refactor / Inline Function: parameters of lambda as call argument turn incompilable
- [`KT-17223`](https://youtrack.jetbrains.com/issue/KT-17223) Absolute path to Kotlin compiler plugin in IML
- [`KT-17234`](https://youtrack.jetbrains.com/issue/KT-17234) Refactor / Inline on library property is rejected after GUI freeze for a while
- [`KT-17272`](https://youtrack.jetbrains.com/issue/KT-17272) Refactor / Inline Function: unused String literal in parameters is kept (while unsed Int is not)
- [`KT-17273`](https://youtrack.jetbrains.com/issue/KT-17273) Refactor / Inline Function: PIEAE: "Element: class org.jetbrains.kotlin.psi.KtCallExpression because: different providers" at PsiUtilCore.ensureValid()
- [`KT-17293`](https://youtrack.jetbrains.com/issue/KT-17293) Project Structure dialog is opened too slow for a project with a lot of empty gradle modules
- [`KT-17296`](https://youtrack.jetbrains.com/issue/KT-17296) Refactor / Inline Function: UOE at ExpressionReplacementPerformer.findOrCreateBlockToInsertStatement() for call of multi-statement function in declaration
- [`KT-17330`](https://youtrack.jetbrains.com/issue/KT-17330) Inline kotlin function causes an infinite loop
- [`KT-17331`](https://youtrack.jetbrains.com/issue/KT-17331) Frequent long editor freezes
- [`KT-17333`](https://youtrack.jetbrains.com/issue/KT-17333) KotlinChangeInfo retains 132MB of the heap
- [`KT-17372`](https://youtrack.jetbrains.com/issue/KT-17372) Specify explicit lambda signature handles anonymous parameters incorrectly
- [`KT-17383`](https://youtrack.jetbrains.com/issue/KT-17383) Slow editing in Kotlin files If breadcrumbs are enabled in module with many dependencies
- [`KT-17395`](https://youtrack.jetbrains.com/issue/KT-17395) Refactor / Inline Function: arguments passed to lambda turns code to incompilable
- [`KT-17400`](https://youtrack.jetbrains.com/issue/KT-17400) Navigate to impl: implementations are duplicated
- [`KT-17404`](https://youtrack.jetbrains.com/issue/KT-17404) Editor: attempt to pass type parameter as reified argument causes AE "Classifier descriptor of a type should be of type ClassDescriptor" at DescriptorUtils.getClassDescriptorForTypeConstructor()
- [`KT-17408`](https://youtrack.jetbrains.com/issue/KT-17408) "Convert to secondary constructor" intention is available for annotation parameters
- [`KT-17472`](https://youtrack.jetbrains.com/issue/KT-17472) Refactor / Move: another class: Java class could be reported explicitly
- [`KT-17482`](https://youtrack.jetbrains.com/issue/KT-17482) Set jvmTarget to 1.8 by default when configuring a project with JDK 1.8
- [`KT-17495`](https://youtrack.jetbrains.com/issue/KT-17495) Much time spent in LibraryDependenciesCache.getLibrariesAndSdksUsedWith
- [`KT-17496`](https://youtrack.jetbrains.com/issue/KT-17496) Refactor / Move: calls to moved extension function type properties are updated (incorrectly)
- [`KT-17503`](https://youtrack.jetbrains.com/issue/KT-17503) Intention "To raw string literal" should handle string concatenations
- [`KT-17515`](https://youtrack.jetbrains.com/issue/KT-17515) Refactor / Move inner class to another class, Move companion object: disabled in editor, but available in Move dialog
- [`KT-17517`](https://youtrack.jetbrains.com/issue/KT-17517) Compiler options specified as properties are not handled by Maven importer
- [`KT-17520`](https://youtrack.jetbrains.com/issue/KT-17520) Quickfix to update language/API version should work for Maven projects
- [`KT-17521`](https://youtrack.jetbrains.com/issue/KT-17521) Quickfix to enable coroutines should work for Maven projects
- [`KT-17525`](https://youtrack.jetbrains.com/issue/KT-17525) IDE: KNPE at KotlinAddImportActionKt.createSingleImportActionForConstructor() on invalid reference to inner class constructor
- [`KT-17526`](https://youtrack.jetbrains.com/issue/KT-17526) Refactor / Move: reference to companion member gets superfluous companion name in certain cases
- [`KT-17538`](https://youtrack.jetbrains.com/issue/KT-17538) Refactor / Move: moving file with import alias removes alias usage from code
- [`KT-17545`](https://youtrack.jetbrains.com/issue/KT-17545) Refactor / Move: false Problems Detected on moving class using parent's protected class, object
- [`KT-17599`](https://youtrack.jetbrains.com/issue/KT-17599) "Make primary constructor internal" intention is available for annotation class 
- [`KT-17600`](https://youtrack.jetbrains.com/issue/KT-17600) "Make primary constructor private" intention is available for annotation class

### JS
- [`KT-12926`](https://youtrack.jetbrains.com/issue/KT-12926) JS: use # instead of @ when linking to sourcemap from generated code
- [`KT-13577`](https://youtrack.jetbrains.com/issue/KT-13577) Double.hashCode is zero for big numbers
- [`KT-15484`](https://youtrack.jetbrains.com/issue/KT-15484) JS: (node): println with object /number argument leads to "TypeError: Invalid data, chunk must be a string or buffer, not object/number"
- [`KT-16658`](https://youtrack.jetbrains.com/issue/KT-16658) JS: Suspend function with default param value in interface
- [`KT-16717`](https://youtrack.jetbrains.com/issue/KT-16717) KotlinJs - copy() on data class doesn't work with when there is a secondary constructor
- [`KT-16745`](https://youtrack.jetbrains.com/issue/KT-16745) JS: initialize enum fields before calling companion objects's initializer
- [`KT-16951`](https://youtrack.jetbrains.com/issue/KT-16951) JS: coroutine suspension point is not inserted when inlining suspend function with tail call to another suspend function
- [`KT-16979`](https://youtrack.jetbrains.com/issue/KT-16979) Kotlin.js : Intellij test and productions sources produce a AMD module with the same name
- [`KT-17067`](https://youtrack.jetbrains.com/issue/KT-17067) JS: suspendCoroutine not working as expected
- [`KT-17219`](https://youtrack.jetbrains.com/issue/KT-17219) Hexadecimal literals in js(...) argument are replaced by wrong decimal constants
- [`KT-17281`](https://youtrack.jetbrains.com/issue/KT-17281) JS: wrong code generated for a recursive call in suspend function
- [`KT-17446`](https://youtrack.jetbrains.com/issue/KT-17446) JS: incorrect code generated for call to `suspendCoroutineOrReturn` when the same function calls another suspend function
- [`KT-17540`](https://youtrack.jetbrains.com/issue/KT-17540) Incorrect inlining optimization of `also`/`apply` function

### Reflection
- [`KT-14988`](https://youtrack.jetbrains.com/issue/KT-14988) Support running the Kotlin compiler on Java 9
- [`KT-17055`](https://youtrack.jetbrains.com/issue/KT-17055) NPE in hashCode and equals of kotlin.jvm.internal.FunctionReference (on local functions)
- [`KT-17594`](https://youtrack.jetbrains.com/issue/KT-17594) Cache the result of val Class<T>.kotlin: KClass<T>

### Libraries
- [`KT-17453`](https://youtrack.jetbrains.com/issue/KT-17453) Array iterators throw IndexOutOfBoundsException instead of NoSuchElementException

## 1.1.2

### Compiler

#### Front-end

- [`KT-16113`](https://youtrack.jetbrains.com/issue/KT-16113) Support destructuring parameters of suspend lambda with suspend componentX
- [`KT-3805`](https://youtrack.jetbrains.com/issue/KT-3805) Report error on double constants out of range
- [`KT-6014`](https://youtrack.jetbrains.com/issue/KT-6014) Wrong ABSTRACT_MEMBER_NOT_IMPLEMENTED for toString implemented by delegation
- [`KT-8959`](https://youtrack.jetbrains.com/issue/KT-8959) Missing diagnostic when trying to call inner class constructor qualificated with outer class name
- [`KT-12477`](https://youtrack.jetbrains.com/issue/KT-12477) Do not report 'const' inapplicability on property of error type
- [`KT-11010`](https://youtrack.jetbrains.com/issue/KT-11010) NDFDE for local object with type parameters
- [`KT-12881`](https://youtrack.jetbrains.com/issue/KT-12881) Descriptor wasn't found for declaration TYPE_PARAMETER
- [`KT-13342`](https://youtrack.jetbrains.com/issue/KT-13342) Unqualified super call should not resolve to a method of supertype overriden in another supertype
- [`KT-14236`](https://youtrack.jetbrains.com/issue/KT-14236) Allow to use emptyArray in annotation
- [`KT-14536`](https://youtrack.jetbrains.com/issue/KT-14536) IllegalStateException: Type parameter T not found for lazy class Companion at LazyDeclarationResolver visitTypeParameter
- [`KT-14865`](https://youtrack.jetbrains.com/issue/KT-14865) Throwable exception at KotlinParser parseLambdaExpression on typing { inside a string inside a lambda
- [`KT-15516`](https://youtrack.jetbrains.com/issue/KT-15516) Compiler error when passing suspending extension-functions as parameter and casting stuff to Any
- [`KT-15802`](https://youtrack.jetbrains.com/issue/KT-15802) Java constant referenced using subclass is not considered a constant expression
- [`KT-15872`](https://youtrack.jetbrains.com/issue/KT-15872) Constant folding is mistakenly triggered for user function
- [`KT-15901`](https://youtrack.jetbrains.com/issue/KT-15901) Unstable smart cast target after type check
- [`KT-15951`](https://youtrack.jetbrains.com/issue/KT-15951) Callable reference to class constructor from object is not resolved
- [`KT-16232`](https://youtrack.jetbrains.com/issue/KT-16232) Prohibit objects inside inner classes
- [`KT-16233`](https://youtrack.jetbrains.com/issue/KT-16233) Prohibit inner sealed classes
- [`KT-16250`](https://youtrack.jetbrains.com/issue/KT-16250) Import methods from typealias to object throws compiler exception  "Should be class or package: typealias"
- [`KT-16272`](https://youtrack.jetbrains.com/issue/KT-16272) Missing deprecation and SinceKotlin-related diagnostic for variable as function call
- [`KT-16278`](https://youtrack.jetbrains.com/issue/KT-16278) Public member method can't be used for callable reference because of private static with the same name
- [`KT-16372`](https://youtrack.jetbrains.com/issue/KT-16372) 'mod is deprecated' warning should not be shown when language version is 1.0
- [`KT-16484`](https://youtrack.jetbrains.com/issue/KT-16484) SimpleTypeImpl should not be created for error type: ErrorScope
- [`KT-16528`](https://youtrack.jetbrains.com/issue/KT-16528) Error: Loop in supertypes when using Java classes with type parameters having raw interdependent supertypes
- [`KT-16538`](https://youtrack.jetbrains.com/issue/KT-16538) No smart cast when equals is present
- [`KT-16782`](https://youtrack.jetbrains.com/issue/KT-16782) Enum entry is incorrectly forbidden on LHS of '::' with language version 1.0
- [`KT-16815`](https://youtrack.jetbrains.com/issue/KT-16815) Assertion error from compiler: unexpected classifier: class DeserializedTypeAliasDescriptor
- [`KT-16931`](https://youtrack.jetbrains.com/issue/KT-16931) Compiler cannot see inner class when for outer class exist folder with same name
- [`KT-16956`](https://youtrack.jetbrains.com/issue/KT-16956) Prohibit using function calls inside default parameter values of annotations
- [`KT-8187`](https://youtrack.jetbrains.com/issue/KT-8187) IAE on anonymous object in the delegation specifier list
- [`KT-8813`](https://youtrack.jetbrains.com/issue/KT-8813) Do not report unused parameters for anonymous functions
- [`KT-12112`](https://youtrack.jetbrains.com/issue/KT-12112) Do not consider nullability of error functions and properties for smart casts
- [`KT-12276`](https://youtrack.jetbrains.com/issue/KT-12276) No warning for unnecessary non-null assertion after method call with generic return type
- [`KT-13648`](https://youtrack.jetbrains.com/issue/KT-13648) Spurious warning: "Elvis operator (?:) always returns the left operand of non-nullable type (???..???)"
- [`KT-16264`](https://youtrack.jetbrains.com/issue/KT-16264) Forbid usage of _ without backticks
- [`KT-16875`](https://youtrack.jetbrains.com/issue/KT-16875) Decrease severity of unused parameter in lambda to weak warning
- [`KT-17136`](https://youtrack.jetbrains.com/issue/KT-17136) ModuleDescriptorImpl.allImplementingModules should be evaluated lazily
- [`KT-17214`](https://youtrack.jetbrains.com/issue/KT-17214) Do not show warning about useless elvis for error function types
- [`KT-13740`](https://youtrack.jetbrains.com/issue/KT-13740) Plugin crashes at accidentally wrong annotation argument type
- [`KT-17597`](https://youtrack.jetbrains.com/issue/KT-17597) Pattern::compile resolves to private instance method in 1.1.2

#### Back-end

- [`KT-8689`](https://youtrack.jetbrains.com/issue/KT-8689) NoSuchMethodError on local functions inside inlined lambda with variables captured from outer context
- [`KT-11314`](https://youtrack.jetbrains.com/issue/KT-11314) Abstract generic class with Array<Array<T>> parameter compiles fine but fails at runtime with "Bad type on operand stack" VerifyError
- [`KT-12839`](https://youtrack.jetbrains.com/issue/KT-12839) Two null checks are generated when manually null checking platform type
- [`KT-14565`](https://youtrack.jetbrains.com/issue/KT-14565) Cannot pop operand off empty stack when compiling enum class
- [`KT-14566`](https://youtrack.jetbrains.com/issue/KT-14566) Make kotlin.jvm.internal.Ref$...Ref classes serializable
- [`KT-14567`](https://youtrack.jetbrains.com/issue/KT-14567) VerifyError: Bad type on operand stack (generics with operator methods)
- [`KT-14607`](https://youtrack.jetbrains.com/issue/KT-14607) Incorrect class name "ava/lang/Void from AsyncTask extension function
- [`KT-14811`](https://youtrack.jetbrains.com/issue/KT-14811) Unecessary checkcast generated in parameterized functions.
- [`KT-14963`](https://youtrack.jetbrains.com/issue/KT-14963) unnecessary checkcast java/lang/Object
- [`KT-15105`](https://youtrack.jetbrains.com/issue/KT-15105) Comparing Chars in a Pair results in ClassCastException
- [`KT-15109`](https://youtrack.jetbrains.com/issue/KT-15109) Subclass from a type alias with named parameter in constructor will produce compiler exception
- [`KT-15192`](https://youtrack.jetbrains.com/issue/KT-15192) Compiler crashes on certain companion objects: "Error generating constructors of class Companion with kind IMPLEMENTATION"
- [`KT-15424`](https://youtrack.jetbrains.com/issue/KT-15424) javac crash when calling Kotlin function having generic varargs with default and @JvmOverloads
- [`KT-15574`](https://youtrack.jetbrains.com/issue/KT-15574) Can't instantiate Array through Type Alias
- [`KT-15594`](https://youtrack.jetbrains.com/issue/KT-15594) java.lang.VerifyError when referencing normal getter in @JvmStatic getters inside an object
- [`KT-15759`](https://youtrack.jetbrains.com/issue/KT-15759) tailrec suspend function fails to compile
- [`KT-15862`](https://youtrack.jetbrains.com/issue/KT-15862) Inline generic functions can unexpectedly box primitives
- [`KT-15871`](https://youtrack.jetbrains.com/issue/KT-15871) Unnecessary boxing for equality operator on inlined primitive values
- [`KT-15993`](https://youtrack.jetbrains.com/issue/KT-15993) Property annotations are stored in private fields and killed by obfuscators
- [`KT-15997`](https://youtrack.jetbrains.com/issue/KT-15997) Reified generics don't work properly with crossinline functions
- [`KT-16077`](https://youtrack.jetbrains.com/issue/KT-16077) Redundant private getter for private var in a class within a JvmMultifileClass annotated file
- [`KT-16194`](https://youtrack.jetbrains.com/issue/KT-16194) Code with unnecessary safe call contains redundant boxing/unboxing for primitive values
- [`KT-16245`](https://youtrack.jetbrains.com/issue/KT-16245) Redundant null-check generated for a cast of already non-nullable value
- [`KT-16532`](https://youtrack.jetbrains.com/issue/KT-16532) Kotlin 1.1 RC - Android cross-inline synchronized won't run
- [`KT-16555`](https://youtrack.jetbrains.com/issue/KT-16555) VerifyError: Bad type on operand stack
- [`KT-16713`](https://youtrack.jetbrains.com/issue/KT-16713) Insufficient maximum stack size
- [`KT-16720`](https://youtrack.jetbrains.com/issue/KT-16720) ClassCastException during compilation
- [`KT-16732`](https://youtrack.jetbrains.com/issue/KT-16732) Type 'java/lang/Number' (current frame, stack[0]) is not assignable to 'java/lang/Character
- [`KT-16929`](https://youtrack.jetbrains.com/issue/KT-16929) `VerifyError` when using bound method reference on generic property
- [`KT-16412`](https://youtrack.jetbrains.com/issue/KT-16412) Exception from compiler when try call SAM constructor where argument is callable reference to nested class inside object
- [`KT-17210`](https://youtrack.jetbrains.com/issue/KT-17210) Smartcast failure results in "Bad type operand on stack" runtime error

### Tools

- [`KT-15420`](https://youtrack.jetbrains.com/issue/KT-15420) Maven, all-open plugin: in console the settings of all-open are always reported as empty
- [`KT-11916`](https://youtrack.jetbrains.com/issue/KT-11916) Provide incremental compilation for Maven
- [`KT-15946`](https://youtrack.jetbrains.com/issue/KT-15946) Kotlin-JPA plugin support for @Embeddable
- [`KT-16627`](https://youtrack.jetbrains.com/issue/KT-16627) Do not make private members open in all-open plugin
- [`KT-16699`](https://youtrack.jetbrains.com/issue/KT-16699) Script resolving doesn't work with custom templates located in an external jar
- [`KT-16812`](https://youtrack.jetbrains.com/issue/KT-16812) import in .kts file does not works
- [`KT-16927`](https://youtrack.jetbrains.com/issue/KT-16927) Using `KotlinJsr223JvmLocalScriptEngineFactory` causes multiple warnings
- [`KT-15562`](https://youtrack.jetbrains.com/issue/KT-15562) Service is dying
- [`KT-17125`](https://youtrack.jetbrains.com/issue/KT-17125) > Failed to apply plugin [id 'kotlin'] > For input string: “”

#### Kapt

- [`KT-12432`](https://youtrack.jetbrains.com/issue/KT-12432) Dagger 2 does not generate Component which was referenced from Kotlin file.
- [`KT-8558`](https://youtrack.jetbrains.com/issue/KT-8558) KAPT only works with service-declared annotation processors
- [`KT-16753`](https://youtrack.jetbrains.com/issue/KT-16753) kapt3 generates invalid stubs when IC is enabled
- [`KT-16458`](https://youtrack.jetbrains.com/issue/KT-16458) kotlin-kapt / kapt3:  "cannot find symbol" error for companion object with same name as enclosing class
- [`KT-14478`](https://youtrack.jetbrains.com/issue/KT-14478) Add APT / Kapt support to the maven plugin
- [`KT-14070`](https://youtrack.jetbrains.com/issue/KT-14070) Kapt3: kapt doesn't compile generated Kotlin files and doesn't use the "kapt.kotlin.generated" folder anymore
- [`KT-16990`](https://youtrack.jetbrains.com/issue/KT-16990) Kapt3: java.io.File cannot be cast to java.lang.String
- [`KT-16965`](https://youtrack.jetbrains.com/issue/KT-16965) Error:Kotlin: Multiple values are not allowed for plugin option org.jetbrains.kotlin.kapt:output
- [`KT-16184`](https://youtrack.jetbrains.com/issue/KT-16184) AbstractMethodError in Kapt3ComponentRegistrar while compiling from IntelliJ  2016.3.4 using Kotlin 1.1.0-beta-38

#### Gradle

- [`KT-15084`](https://youtrack.jetbrains.com/issue/KT-15084) Navigation into sources of gradle-script-kotlin doesn't work
- [`KT-16003`](https://youtrack.jetbrains.com/issue/KT-16003) Gradle Plugin Fails When Run From Jenkins On Multiple Nodes
- [`KT-16585`](https://youtrack.jetbrains.com/issue/KT-16585) Kotlin Gradle Plugin makes using Gradle Java incremental compiler not work
- [`KT-16902`](https://youtrack.jetbrains.com/issue/KT-16902) Gradle plugin compilation on daemon fails on Linux ARM
- [`KT-14619`](https://youtrack.jetbrains.com/issue/KT-14619) Gradle: The '-d' option with a directory destination is ignored because '-module' is specified
- [`KT-12792`](https://youtrack.jetbrains.com/issue/KT-12792) Automatically configure standard library dependency and set its version equal to compiler version if not specified
- [`KT-15994`](https://youtrack.jetbrains.com/issue/KT-15994) Compiler arguments are not copied from the main compile task to kapt task
- [`KT-16820`](https://youtrack.jetbrains.com/issue/KT-16820) Changing compileKotlin.destinationDir leads to failure in :copyMainKotlinClasses task due to an NPE
- [`KT-16917`](https://youtrack.jetbrains.com/issue/KT-16917) First connection to daemon after start timeouts when DNS is slow
- [`KT-16580`](https://youtrack.jetbrains.com/issue/KT-16580) Kotlin gradle plugin cannot resolve the kotlin compiler 

### Android support

- [`KT-16624`](https://youtrack.jetbrains.com/issue/KT-16624) Implement quickfix "Add TargetApi/RequiresApi annotation" for Android api issues
- [`KT-16625`](https://youtrack.jetbrains.com/issue/KT-16625) Implement quickfix "Surround with if (VERSION.SDK_INT >= VERSION_CODES.SOME_VERSION) { ... }" for Android api issues
- [`KT-16840`](https://youtrack.jetbrains.com/issue/KT-16840) Kotlin Gradle plugin fails with Android Gradle plugin 2.4.0-alpha1
- [`KT-16897`](https://youtrack.jetbrains.com/issue/KT-16897) Gradle plugin 1.1.1 duplicates all main classes into Android instrumentation test APK
- [`KT-16957`](https://youtrack.jetbrains.com/issue/KT-16957) Android Extensions: Support Dialog class
- [`KT-15023`](https://youtrack.jetbrains.com/issue/KT-15023) Android `gradle installDebugAndroidTest` fails unless you first call `gradle assembleDebugAndroidTest`
- [`KT-12769`](https://youtrack.jetbrains.com/issue/KT-12769) "Name for method must be provided" error occurs on trying to use spaces in method name in integration tests in Android
- [`KT-12819`](https://youtrack.jetbrains.com/issue/KT-12819) Kotlin Lint: False positive for "Unconditional layout inflation" when using elvis operator
- [`KT-15116`](https://youtrack.jetbrains.com/issue/KT-15116) Kotlin Lint: problems in property accessors are not reported
- [`KT-15156`](https://youtrack.jetbrains.com/issue/KT-15156) Kotlin Lint: problems in annotation parameters are not reported
- [`KT-15179`](https://youtrack.jetbrains.com/issue/KT-15179) Kotlin Lint: problems inside local function are not reported
- [`KT-14870`](https://youtrack.jetbrains.com/issue/KT-14870) Kotlin Lint: problems inside local class are not reported
- [`KT-14920`](https://youtrack.jetbrains.com/issue/KT-14920) Kotlin Lint: "Android Lint for Kotlin | Incorrect support annotation usage" inspection does not report problems
- [`KT-14947`](https://youtrack.jetbrains.com/issue/KT-14947) Kotlin Lint: "Calling new methods on older versions" could suggest specific quick fixes
- [`KT-12741`](https://youtrack.jetbrains.com/issue/KT-12741) Android Extensions: Enable IDE plugin only if it is enabled in the build.gradle file
- [`KT-13122`](https://youtrack.jetbrains.com/issue/KT-13122) Implement '@RequiresApi' intention for android and don't report warning on annotated classes
- [`KT-16680`](https://youtrack.jetbrains.com/issue/KT-16680) Stack overflow in UAST containsLocalTypes()
- [`KT-15451`](https://youtrack.jetbrains.com/issue/KT-15451) Support "Android String Reference" folding in Kotlin files
- [`KT-16132`](https://youtrack.jetbrains.com/issue/KT-16132) Renaming property provided by kotlinx leads to renaming another members
- [`KT-17200`](https://youtrack.jetbrains.com/issue/KT-17200) Unable to build an android project
- [`KT-13104`](https://youtrack.jetbrains.com/issue/KT-13104) Incorrect resource name in Activity after renaming ID attribute value in layout file
- [`KT-17436`](https://youtrack.jetbrains.com/issue/KT-17436) Refactor | Rename android:id corrupts R.id references in kotlin code
- [`KT-17255`](https://youtrack.jetbrains.com/issue/KT-17255) Kotlin 1.1.2 EAP is broken with 2.4.0-alpha3
- [`KT-17610`](https://youtrack.jetbrains.com/issue/KT-17610) "Unknown reference: kotlinx"

### IDE

- [`KT-6159`](https://youtrack.jetbrains.com/issue/KT-6159) Inline Method refactoring
- [`KT-4578`](https://youtrack.jetbrains.com/issue/KT-4578) Intention to move property between class body and constructor parameter
- [`KT-8568`](https://youtrack.jetbrains.com/issue/KT-8568) Provide a QuickFix to replace type `Array<Int>` in annotation with `IntArray`
- [`KT-10393`](https://youtrack.jetbrains.com/issue/KT-10393) Detect calls to functions returning a lambda from expression body which ignore the return value
- [`KT-11393`](https://youtrack.jetbrains.com/issue/KT-11393) Inspection to highlight and warn on usage of internal members in other module from Java
- [`KT-12004`](https://youtrack.jetbrains.com/issue/KT-12004) IDE inspection that destructuring variable name matches the other name in data class
- [`KT-12183`](https://youtrack.jetbrains.com/issue/KT-12183) Intention converting several calls with same receiver to 'with'/`apply`/`run`
- [`KT-13111`](https://youtrack.jetbrains.com/issue/KT-13111) Support bound references in lambda-to-reference intention / inspection
- [`KT-15966`](https://youtrack.jetbrains.com/issue/KT-15966) Create quickfix for DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE
- [`KT-16074`](https://youtrack.jetbrains.com/issue/KT-16074) Introduce a quick-fix adding noinline modifier for a value parameter of suspend function type 
- [`KT-16131`](https://youtrack.jetbrains.com/issue/KT-16131) Add quickfix for: Cannot access member: it is invisible (private in supertype)
- [`KT-16188`](https://youtrack.jetbrains.com/issue/KT-16188) Add create class quickfix
- [`KT-16258`](https://youtrack.jetbrains.com/issue/KT-16258) Add intention to add missing components to destructuring assignment
- [`KT-16292`](https://youtrack.jetbrains.com/issue/KT-16292) Support "Reference to lambda" for bound references
- [`KT-11234`](https://youtrack.jetbrains.com/issue/KT-11234) Debugger won't hit breakpoint in nested lamba
- [`KT-12002`](https://youtrack.jetbrains.com/issue/KT-12002) Improve completion for closure parameters to work in more places
- [`KT-15768`](https://youtrack.jetbrains.com/issue/KT-15768) It would be nice to show in Kotlin facet what compiler plugins are on and their options
- [`KT-16022`](https://youtrack.jetbrains.com/issue/KT-16022) Kotlin facet: provide UI to navigate to project settings
- [`KT-16214`](https://youtrack.jetbrains.com/issue/KT-16214) Do not hide package kotlin.reflect.jvm.internal from auto-import and completion, inside package "kotlin.reflect"
- [`KT-16647`](https://youtrack.jetbrains.com/issue/KT-16647) Don't create kotlinc.xml if the settings don't differ from the defaults
- [`KT-16649`](https://youtrack.jetbrains.com/issue/KT-16649) All Gradle related classes should be moved to optional dependency section of plugin.xml
- [`KT-16800`](https://youtrack.jetbrains.com/issue/KT-16800) Autocomplete for closure with single arguments

#### Bug fixes

- [`KT-16316`](https://youtrack.jetbrains.com/issue/KT-16316) IDE: don't show Kotlin Scripting section when target platform is JavaScript
- [`KT-16317`](https://youtrack.jetbrains.com/issue/KT-16317) IDE: some fields stay enabled in an facet when use project settings was chosen
- [`KT-16596`](https://youtrack.jetbrains.com/issue/KT-16596) Hang in IntelliJ while scanning zips
- [`KT-16646`](https://youtrack.jetbrains.com/issue/KT-16646) The flag to enable coroutines does not sync from gradle file in Android Studio
- [`KT-16788`](https://youtrack.jetbrains.com/issue/KT-16788) Importing Kotlin Maven projects results in invalid .iml
- [`KT-16827`](https://youtrack.jetbrains.com/issue/KT-16827) kotlin javascript module not recognized by gradle sync when an android module is present
- [`KT-16848`](https://youtrack.jetbrains.com/issue/KT-16848) Regression: completion after dot in string interpolation expression doesn't work if there are no curly braces
- [`KT-16888`](https://youtrack.jetbrains.com/issue/KT-16888) "Multiple values are not allowed for plugin option org.jetbrains.kotlin.android:package" when rebuilding project
- [`KT-16980`](https://youtrack.jetbrains.com/issue/KT-16980) Accessing language version settings for a module performs runtime version detection on every access with no caching
- [`KT-16991`](https://youtrack.jetbrains.com/issue/KT-16991) Navigate to receiver from this in extension function
- [`KT-16992`](https://youtrack.jetbrains.com/issue/KT-16992) Navigate to lambda start from auto-generated 'it' parameter 
- [`KT-12264`](https://youtrack.jetbrains.com/issue/KT-12264) AssertionError: Resolver for 'completion/highlighting in LibrarySourceInfo for platform JVM' does not know how to resolve ModuleProductionSourceInfo
- [`KT-13734`](https://youtrack.jetbrains.com/issue/KT-13734) Annotated element search is slow
- [`KT-14710`](https://youtrack.jetbrains.com/issue/KT-14710) Sample references aren't resolved in IDE
- [`KT-16415`](https://youtrack.jetbrains.com/issue/KT-16415) Dependency leakage with Kotlin IntelliJ plugin, using gradle-script-kotlin, and the gradle-intellij-plugin
- [`KT-16837`](https://youtrack.jetbrains.com/issue/KT-16837) Slow typing in Kotlin file because of ImportFixBase
- [`KT-16926`](https://youtrack.jetbrains.com/issue/KT-16926) 'implement' dependency is not transitive when importing gradle project to IDEA
- [`KT-17141`](https://youtrack.jetbrains.com/issue/KT-17141) Running test from gutter icon fails in AS 2.4 Preview 3
- [`KT-17162`](https://youtrack.jetbrains.com/issue/KT-17162) Plain-text Java copy-paste to Kotlin file results in exception
- [`KT-16714`](https://youtrack.jetbrains.com/issue/KT-16714) J2K: Write access is allowed from event dispatch thread only
- [`KT-14058`](https://youtrack.jetbrains.com/issue/KT-14058) Unexpected error MISSING_DEPENDENCY_CLASS
- [`KT-9275`](https://youtrack.jetbrains.com/issue/KT-9275) Unhelpful IDE warning "Configure Kotlin"
- [`KT-15279`](https://youtrack.jetbrains.com/issue/KT-15279) 'Kotlin not configured message' should not be displayed while gradle sync is in progress
- [`KT-11828`](https://youtrack.jetbrains.com/issue/KT-11828) Configure Kotlin in Project: failure for Gradle modules without build.gradle (IDEA creates them)
- [`KT-16571`](https://youtrack.jetbrains.com/issue/KT-16571) Configure Kotlin in Project does not suggest just published version
- [`KT-16590`](https://youtrack.jetbrains.com/issue/KT-16590) Configure kotlin warning popup after each sync gradle
- [`KT-16353`](https://youtrack.jetbrains.com/issue/KT-16353) Configure Kotlin in Project: configurators are not suggested for Gradle module in non-Gradle project with separate sub-modules for source sets
- [`KT-16381`](https://youtrack.jetbrains.com/issue/KT-16381) Configure Kotlin dialog suggests modules already configured with other platforms
- [`KT-16401`](https://youtrack.jetbrains.com/issue/KT-16401) Configure Kotlin in the project adds incorrect dependency kotlin-stdlib-jre8 to 1.0.x language
- [`KT-12261`](https://youtrack.jetbrains.com/issue/KT-12261) Partial body resolve doesn't resolve anything in object literal used as an expression body of a method
- [`KT-13013`](https://youtrack.jetbrains.com/issue/KT-13013) "Go to Type Declaration" doesn't work for extension receiver and implict lambda parameter
- [`KT-13135`](https://youtrack.jetbrains.com/issue/KT-13135) IDE goes in an infinite indexing loop if a .kotlin_module file is corrupted
- [`KT-14129`](https://youtrack.jetbrains.com/issue/KT-14129) for/iter postfix templates should be applied for string, ranges and mutable collections
- [`KT-14134`](https://youtrack.jetbrains.com/issue/KT-14134) Allow to apply for/iter postfix template to map
- [`KT-14871`](https://youtrack.jetbrains.com/issue/KT-14871) Idea and Maven is not in sync with ModuleKind for Kotlin projects
- [`KT-14986`](https://youtrack.jetbrains.com/issue/KT-14986) Disable postfix completion when typing package statements
- [`KT-15200`](https://youtrack.jetbrains.com/issue/KT-15200) Show implementation should show inherited classes if a typealias to base class/interface is used
- [`KT-15398`](https://youtrack.jetbrains.com/issue/KT-15398) Annotations find usages in annotation instance site
- [`KT-15536`](https://youtrack.jetbrains.com/issue/KT-15536) Highlight usages: Class with primary constructor isn't highlighted when caret is on constructor invocation
- [`KT-15628`](https://youtrack.jetbrains.com/issue/KT-15628) Change error message if both KotlinJavaRuntime and KotlinJavaScript libraries are present in the module dependencies
- [`KT-15947`](https://youtrack.jetbrains.com/issue/KT-15947) Kotlin facet: Target platform on importing from a maven project should be filled the same way for different artifacts
- [`KT-16023`](https://youtrack.jetbrains.com/issue/KT-16023) Kotlin facet: When "Use project settings" is enabled, respective fields should show values from the project settings
- [`KT-16698`](https://youtrack.jetbrains.com/issue/KT-16698) Kotlin facet: modules created from different gradle sourcesets have the same module options
- [`KT-16700`](https://youtrack.jetbrains.com/issue/KT-16700) Kotlin facet: jdkHome path containing spaces splits into several additional args after import
- [`KT-16776`](https://youtrack.jetbrains.com/issue/KT-16776) Kotlin facet, import from maven: free arguments from submodule doesn't override arguments from parent module
- [`KT-16550`](https://youtrack.jetbrains.com/issue/KT-16550) Kotlin facet from Maven: provide error messages if additional command line parameters are set several times
- [`KT-16313`](https://youtrack.jetbrains.com/issue/KT-16313) Kotlin facet: unify filling up information about included AllOpen/NoArg plugins on importing from Maven and Gradle
- [`KT-16342`](https://youtrack.jetbrains.com/issue/KT-16342) Kotlin facet: JavaScript platform is not detected if there are 2 versions of stdlib in dependencies
- [`KT-16032`](https://youtrack.jetbrains.com/issue/KT-16032) Kotlin code formatter merges comment line with non-comment line
- [`KT-16038`](https://youtrack.jetbrains.com/issue/KT-16038) UI blocked on pasting java code into a kotlin file
- [`KT-16062`](https://youtrack.jetbrains.com/issue/KT-16062) Kotlin breakpoint doesn't work in some lambda in Rider project.
- [`KT-15855`](https://youtrack.jetbrains.com/issue/KT-15855) Can't evaluate expression in @JvmStatic method
- [`KT-16667`](https://youtrack.jetbrains.com/issue/KT-16667) Kotlin debugger "smart step into" fail on method defined in the middle of class hierarchy
- [`KT-16078`](https://youtrack.jetbrains.com/issue/KT-16078) Formatter puts empty body braces on different lines when KDoc is present
- [`KT-16265`](https://youtrack.jetbrains.com/issue/KT-16265) Parameter info doesn't work with type alias constructor
- [`KT-14727`](https://youtrack.jetbrains.com/issue/KT-14727) Wrong samples for some postfix templates

##### Inspections / Quickfixes

- [`KT-17002`](https://youtrack.jetbrains.com/issue/KT-17002) Make "Lambda to Reference" inspection off by default
- [`KT-14402`](https://youtrack.jetbrains.com/issue/KT-14402) Inspection "Use destructuring declaration" for lambdas doesn't work when parameter is of type Pair
- [`KT-16857`](https://youtrack.jetbrains.com/issue/KT-16857) False "Remove redundant 'let'" suggestion
- [`KT-16928`](https://youtrack.jetbrains.com/issue/KT-16928) Surround with null check quickfix works badly in case of qualifier
- [`KT-15870`](https://youtrack.jetbrains.com/issue/KT-15870) Move quick fix of "Package name does not match containing directory" inspection: Throwable "AWT events are not allowed inside write action"
- [`KT-16128`](https://youtrack.jetbrains.com/issue/KT-16128) 'Add label to loop' QF proposed when there's already a label
- [`KT-16828`](https://youtrack.jetbrains.com/issue/KT-16828) Don't suggest destructing declarations if not all components are used
- [`KT-17022`](https://youtrack.jetbrains.com/issue/KT-17022) Replace deprecated in the whole project may miss some usages in expression body

##### Refactorings, Intentions

- [`KT-7516`](https://youtrack.jetbrains.com/issue/KT-7516) Rename refactoring doesn't rename related labels
- [`KT-7520`](https://youtrack.jetbrains.com/issue/KT-7520) Exception when try rename label from usage
- [`KT-8955`](https://youtrack.jetbrains.com/issue/KT-8955) Refactor / Move package: KNPE at KotlinMoveDirectoryWithClassesHelper.postProcessUsages() with not matching package statement
- [`KT-11863`](https://youtrack.jetbrains.com/issue/KT-11863) Refactor / Move: moving referred file level elements to another package keeps reference to old FQN
- [`KT-13190`](https://youtrack.jetbrains.com/issue/KT-13190) Refactor / Move: no warning on moving class containing internal member to different module
- [`KT-13341`](https://youtrack.jetbrains.com/issue/KT-13341) Convert lambda to function reference intention is not available for object member calls
- [`KT-13755`](https://youtrack.jetbrains.com/issue/KT-13755) When (java?) class is moved redundant imports are not removed
- [`KT-13911`](https://youtrack.jetbrains.com/issue/KT-13911) Refactor / Move: "Problems Detected" dialog is not shown on moving whole .kt file
- [`KT-14401`](https://youtrack.jetbrains.com/issue/KT-14401) Can't rename implicit lambda parameter 'it' when caret is placed right after the last character
- [`KT-14483`](https://youtrack.jetbrains.com/issue/KT-14483) "Argument of NotNull parameter must be not null" in KotlinTryCatchSurrounder when using "try" postfix template
- [`KT-15075`](https://youtrack.jetbrains.com/issue/KT-15075) KNPE in "Specify explicit lambda signature"
- [`KT-15190`](https://youtrack.jetbrains.com/issue/KT-15190) Refactor / Move: false Problems Detected on moving class using parent's protected member
- [`KT-15250`](https://youtrack.jetbrains.com/issue/KT-15250) Convert anonymous object to lambda is shown when conversion not possible due implicit calls on this
- [`KT-15339`](https://youtrack.jetbrains.com/issue/KT-15339) Extract Superclass is enabled for any element: CommonRefactoringUtil$RefactoringErrorHintException: "Superclass cannot be extracted from interface" at ExtractSuperRefactoring.performRefactoring()
- [`KT-15559`](https://youtrack.jetbrains.com/issue/KT-15559) Kotlin: Moving classes to different packages breaks references to companion object's properties
- [`KT-15556`](https://youtrack.jetbrains.com/issue/KT-15556) Convert lambda to reference isn't proposed for parameterless constructor
- [`KT-15586`](https://youtrack.jetbrains.com/issue/KT-15586) ISE during "Move to a separate file"
- [`KT-15822`](https://youtrack.jetbrains.com/issue/KT-15822) Move class refactoring leaves unused imports
- [`KT-16108`](https://youtrack.jetbrains.com/issue/KT-16108) Cannot rename class on the companion object reference
- [`KT-16198`](https://youtrack.jetbrains.com/issue/KT-16198) Extract method refactoring should order parameters by first usage
- [`KT-17006`](https://youtrack.jetbrains.com/issue/KT-17006) Refactor / Move: usage of library function is reported as problem on move between modules with different library versions
- [`KT-17032`](https://youtrack.jetbrains.com/issue/KT-17032) Refactor / Move updates references to not moved class from the same file
- [`KT-11907`](https://youtrack.jetbrains.com/issue/KT-11907) Move to package renames file to temp.kt
- [`KT-16468`](https://youtrack.jetbrains.com/issue/KT-16468) Destructure declaration intention should be applicable for Pair
- [`KT-16162`](https://youtrack.jetbrains.com/issue/KT-16162) IAE for destructuring declaration entry from KotlinFinalClassOrFunSpringInspection
- [`KT-16556`](https://youtrack.jetbrains.com/issue/KT-16556) Move refactoring shows Refactoring cannot be performed warning.
- [`KT-16605`](https://youtrack.jetbrains.com/issue/KT-16605) NPE caused by Rename Refactoring of backing field when caret is after the last character
- [`KT-16809`](https://youtrack.jetbrains.com/issue/KT-16809) Move refactoring fails badly
- [`KT-16903`](https://youtrack.jetbrains.com/issue/KT-16903) "Convert to primary constructor" doesn't update supertype constructor call in supertypes list in case of implicit superclass constructor call

### JS

- [`KT-6627`](https://youtrack.jetbrains.com/issue/KT-6627) JS: test sources doesn't compile from IDE
- [`KT-13610`](https://youtrack.jetbrains.com/issue/KT-13610) JS: boxed Double.NaN is not equal to itself
- [`KT-16012`](https://youtrack.jetbrains.com/issue/KT-16012) JS: prohibit nested declarations, except interfaces inside external interface
- [`KT-16043`](https://youtrack.jetbrains.com/issue/KT-16043) IDL: mark inline helper function as InlineOnly
- [`KT-16058`](https://youtrack.jetbrains.com/issue/KT-16058) JS: getValue/setValue don't work if they are declared as suspend
- [`KT-16164`](https://youtrack.jetbrains.com/issue/KT-16164) JS: Bad getCallableRef in suspend function
- [`KT-16350`](https://youtrack.jetbrains.com/issue/KT-16350) KotlinJS - wrong code generated when temporary variables generated for RHS of `&&` operation
- [`KT-16377`](https://youtrack.jetbrains.com/issue/KT-16377) JS: losing declarations of temporary variables in secondary constructors
- [`KT-16545`](https://youtrack.jetbrains.com/issue/KT-16545) JS: ::class crashes at runtime for primitive types (e.g. Int::class, or Double::class)
- [`KT-16144`](https://youtrack.jetbrains.com/issue/KT-16144) JS: inliner can't find function called through inheritor ("fake" override) from another module

### Reflection

- [`KT-9453`](https://youtrack.jetbrains.com/issue/KT-9453) ClassCastException: java.lang.Class cannot be cast to kotlin.reflect.KClass
- [`KT-11254`](https://youtrack.jetbrains.com/issue/KT-11254) Make callable references Serializable on JVM
- [`KT-11316`](https://youtrack.jetbrains.com/issue/KT-11316) NPE in hashCode of KProperty object created for delegated property
- [`KT-12630`](https://youtrack.jetbrains.com/issue/KT-12630) KotlinReflectionInternalError on referencing some functions from stdlib
- [`KT-14731`](https://youtrack.jetbrains.com/issue/KT-14731) When starting application from test source root, kotlin function reflection fails in objects defined in sources

### Libraries

- [`KT-16922`](https://youtrack.jetbrains.com/issue/KT-16922) buildSequence/Iterator: Infinite sequence terminates prematurely
- [`KT-16923`](https://youtrack.jetbrains.com/issue/KT-16923) Progression iterator doesn't throw after completion
- [`KT-16994`](https://youtrack.jetbrains.com/issue/KT-16994) Classify sequence operations as stateful/stateless and intermediate/terminal
- [`KT-9786`](https://youtrack.jetbrains.com/issue/KT-9786) String.trimIndent doc is misleading
- [`KT-16572`](https://youtrack.jetbrains.com/issue/KT-16572) Add links to Mozilla Developer Network to kdocs of classes that we generate from IDL
- [`KT-16252`](https://youtrack.jetbrains.com/issue/KT-16252) IDL2K: Add ItemArrayLike interface implementation to collection-like classes

## 1.1.1

### IDE
- [`KT-16714`](https://youtrack.jetbrains.com/issue/KT-16714) J2K: Write access is allowed from event dispatch thread only

### Compiler
- [`KT-16801`](https://youtrack.jetbrains.com/issue/KT-16801) Accessors of `@PublishedApi` property gets mangled
- [`KT-16673`](https://youtrack.jetbrains.com/issue/KT-16673) Potentially problematic code causes exception when work with SAM adapters

### Libraries
- [`KT-16557`](https://youtrack.jetbrains.com/issue/KT-16557) Correct `SinceKotlin(1.1)` for all declarations in `kotlin.reflect.full`

## 1.1.1-RC

### IDE
- [`KT-16481`](https://youtrack.jetbrains.com/issue/KT-16481) Kotlin debugger & bytecode fail on select statement blocks (IllegalStateException: More than one package fragment)

### Gradle support
- [`KT-15783`](https://youtrack.jetbrains.com/issue/KT-15783) Gradle builds don't use incremental compilation due to an error: "Could not connect to kotlin daemon"
- [`KT-16434`](https://youtrack.jetbrains.com/issue/KT-16434) Gradle plugin does not compile androidTest sources when Jack is enabled
- [`KT-16546`](https://youtrack.jetbrains.com/issue/KT-16546) Enable incremental compilation in gradle by default

### Compiler
- [`KT-16184`](https://youtrack.jetbrains.com/issue/KT-16184) AbstractMethodError in Kapt3ComponentRegistrar while compiling from IntelliJ using Kotlin 1.1.0
- [`KT-16578`](https://youtrack.jetbrains.com/issue/KT-16578) Fix substitutor for synthetic SAM adapters
- [`KT-16581`](https://youtrack.jetbrains.com/issue/KT-16581) VerifyError when calling default value parameter with jvm-target 1.8
- [`KT-16583`](https://youtrack.jetbrains.com/issue/KT-16583) Cannot access private file-level variables inside a class init within the same file if a secondary constructor is present
- [`KT-16587`](https://youtrack.jetbrains.com/issue/KT-16587) AbstractMethodError: Delegates not generated correctly for private interfaces
- [`KT-16598`](https://youtrack.jetbrains.com/issue/KT-16598) Incorrect error: The feature "bound callable references" is only available since language version 1.1
- [`KT-16621`](https://youtrack.jetbrains.com/issue/KT-16621) Kotlin compiler doesn't report an error if a class implements Annotation interface but doesn't implement annotationType method
- [`KT-16441`](https://youtrack.jetbrains.com/issue/KT-16441) `NoSuchFieldError: $$delegatedProperties` when delegating through `provideDelegate` in companion object

### JavaScript support
- Prohibit function types with receiver as parameter types of external declarations
- Remove extension receiver for function parameters in `jQuery` declarations

## 1.1

### Compiler exceptions
- [`KT-16411`](https://youtrack.jetbrains.com/issue/KT-16411) Exception from compiler when try to inline callable reference to class constructor inside object
- [`KT-16412`](https://youtrack.jetbrains.com/issue/KT-16412) Exception from compiler when try call SAM constructor where argument is callable reference to nested class inside object
- [`KT-16413`](https://youtrack.jetbrains.com/issue/KT-16413) When we create sam adapter for java.util.function.Function we add incorrect null-check for argument

### Standard library
- [`KT-6561`](https://youtrack.jetbrains.com/issue/KT-6561) Drop java.util.Collections package from js stdlib
- `javaClass` extension property is no more deprecated due to migration problems

### IDE
- [`KT-16329`](https://youtrack.jetbrains.com/issue/KT-16329) Inspection "Calls to staic methods in Java interfaces..." always reports warning undependent of jvm-target


## 1.1-RC

### Reflection
- [`KT-16358`](https://youtrack.jetbrains.com/issue/KT-16358) Incompatibility between kotlin-reflect 1.0 and kotlin-stdlib 1.1 fixed

### Compiler

#### Coroutine support
- [`KT-15938`](https://youtrack.jetbrains.com/issue/KT-15938) Changed error message for calling suspend function outside of suspendable context
- [`KT-16092`](https://youtrack.jetbrains.com/issue/KT-16092) Backend crash fixed: "Don't know how to generate outer expression" for destructuring suspend lambda
- [`KT-16093`](https://youtrack.jetbrains.com/issue/KT-16093) Annotations are retained during reading the binary representation of suspend functions
- [`KT-16122`](https://youtrack.jetbrains.com/issue/KT-16122) java.lang.VerifyError fixed in couroutines: (String, null, suspend () -> String)
- [`KT-16124`](https://youtrack.jetbrains.com/issue/KT-16124) Marked as UNSUPPORTED: suspension points in default parameters
- [`KT-16219`](https://youtrack.jetbrains.com/issue/KT-16219) Marked as UNSUPPORTED: suspend get/set, in/!in operators for
- [`KT-16145`](https://youtrack.jetbrains.com/issue/KT-16145) Beta-2 coroutine regression fixed (wrong code generation)

#### Kapt3
- [`KT-15524`](https://youtrack.jetbrains.com/issue/KT-15524) Fix javac error reporting in Kotlin daemon
- [`KT-15721`](https://youtrack.jetbrains.com/issue/KT-15721) JetBrains nullability annotations are now returned from Element.getAnnotationMirrors()
- [`KT-16146`](https://youtrack.jetbrains.com/issue/KT-16146) Fixed work in verbose mode
- [`KT-16153`](https://youtrack.jetbrains.com/issue/KT-16153) Ignore declarations with illegal Java identifiers
- [`KT-16167`](https://youtrack.jetbrains.com/issue/KT-16167) Fixed compilation error with kapt arguments in build.gradle
- [`KT-16170`](https://youtrack.jetbrains.com/issue/KT-16170) Stub generator now adds imports for corrected error types to stubs
- [`KT-16176`](https://youtrack.jetbrains.com/issue/KT-16176) javac's finalCompiler log is now used to determine annotation processing errors

#### Backward compatibility
- [`KT-16017`](https://youtrack.jetbrains.com/issue/KT-16017) More graceful error message for disabled features
- [`KT-16073`](https://youtrack.jetbrains.com/issue/KT-16073) Improved backward compatibility mode with version 1.0 on JDK dependent built-ins
- [`KT-16094`](https://youtrack.jetbrains.com/issue/KT-16094) Compiler considers API availability when compiling language features requiring runtime support
- [`KT-16171`](https://youtrack.jetbrains.com/issue/KT-16171) Fixed regression "Unexpected container error on Kotlin 1.0 project"
- [`KT-16199`](https://youtrack.jetbrains.com/issue/KT-16199) Do not import "kotlin.comparisons.*" by default in language version 1.0 mode

#### Various issues
- [`KT-16225`](https://youtrack.jetbrains.com/issue/KT-16225) enumValues non-reified stub implementation references nonexistent method no more
- [`KT-16291`](https://youtrack.jetbrains.com/issue/KT-16291) Smart cast works now when getting class of instance
- [`KT-16380`](https://youtrack.jetbrains.com/issue/KT-16380) Show warning when running the compiler under Java 6 or 7

### JavaScript backend
- [`KT-16144`](https://youtrack.jetbrains.com/issue/KT-16144) Fixed inlining of functions called through inheritor ("fake" override) from another module
- [`KT-16158`](https://youtrack.jetbrains.com/issue/KT-16158) Error is not reported now when library path contains JAR file without JS metadata, report warning instead
- [`KT-16160`](https://youtrack.jetbrains.com/issue/KT-16160) Companion object dispatch receiver translation fixed

### Standard library
- [`KT-7858`](https://youtrack.jetbrains.com/issue/KT-7858) Add extension function `takeUnless`
- `javaClass` extension property is deprecated, use `instance::class.java` instead
- Massive deprecations are coming in JS standard library in `kotlin.dom` and `kotlin.dom.build` packages

### IDE

#### Configuration issues
- [`KT-15899`](https://youtrack.jetbrains.com/issue/KT-15899) Kotlin facet: language and api version for submodule setup for 1.0 are filled now as 1.0 too
- [`KT-15914`](https://youtrack.jetbrains.com/issue/KT-15914) Kotlin facet works now with multi-selected modules in Project Settings too
- [`KT-15954`](https://youtrack.jetbrains.com/issue/KT-15954) Does not suggest to configure kotlin for the module after each new kt-file creation
- [`KT-16157`](https://youtrack.jetbrains.com/issue/KT-16157) freeCompilerArgs are now imported from Gradle into IDEA
- [`KT-16206`](https://youtrack.jetbrains.com/issue/KT-16206) Idea no more refuses to compile a kotlin project defined as a maven project
- [`KT-16312`](https://youtrack.jetbrains.com/issue/KT-16312) Kotlin facet: import from gradle: don't import options which are set implicitly already
- [`KT-16325`](https://youtrack.jetbrains.com/issue/KT-16325) Kotlin facet: correct configuration after upgrading the IDE plugin
- [`KT-16345`](https://youtrack.jetbrains.com/issue/KT-16345) Kotlin facet: detect JavaScript if the module has language 1.0 `kotlin-js-library` dependency

#### Coroutine support
- [`KT-16109`](https://youtrack.jetbrains.com/issue/KT-16109) Error fixed: The -Xcoroutines can only have one value 
- [`KT-16251`](https://youtrack.jetbrains.com/issue/KT-16251) Fix detection of suspend calls containing extracted parameters

#### Intention actions, inspections and quick-fixes

##### 2017.1 compatibility
- [`KT-15870`](https://youtrack.jetbrains.com/issue/KT-15870) "Package name does not match containing directory" inspection: fixed throwable "AWT events are not allowed inside write action"
- [`KT-15924`](https://youtrack.jetbrains.com/issue/KT-15924) Create Test action: fixed throwable "AWT events are not allowed inside write action"

##### Bug fixes
- [`KT-14831`](https://youtrack.jetbrains.com/issue/KT-14831) Import statement and FQN are not added on converting lambda to reference for typealias
- [`KT-15545`](https://youtrack.jetbrains.com/issue/KT-15545) Inspection "join with assignment" does not change now execution order for properties
- [`KT-15744`](https://youtrack.jetbrains.com/issue/KT-15744) Fix: intention to import `sleep` wrongly suggests `Thread.sleep`
- [`KT-16000`](https://youtrack.jetbrains.com/issue/KT-16000) Inspection "join with assignment" handles initialization with 'this' correctly
- [`KT-16009`](https://youtrack.jetbrains.com/issue/KT-16009) Auto-import for JDK classes in .kts files
- [`KT-16104`](https://youtrack.jetbrains.com/issue/KT-16104) Don't insert modifiers (e.g. suspend) before visibility

#### Completion
- [`KT-16076`](https://youtrack.jetbrains.com/issue/KT-16076) Completion does not insert more FQN kotlin.text.String
- [`KT-16088`](https://youtrack.jetbrains.com/issue/KT-16088) Completion does not insert more FQN for `kotlin` package
- [`KT-16110`](https://youtrack.jetbrains.com/issue/KT-16110) Keyword 'suspend' completion inside generic arguments
- [`KT-16243`](https://youtrack.jetbrains.com/issue/KT-16243) Performance enhanced after variable of type `ArrayList`

#### Various issues
- [`KT-15291`](https://youtrack.jetbrains.com/issue/KT-15291) 'Find usages' now does not report property access as usage of getter method in Java class with parameter
- [`KT-15647`](https://youtrack.jetbrains.com/issue/KT-15647) Exception fixed: KDoc link to member of class from different package and module
- [`KT-16071`](https://youtrack.jetbrains.com/issue/KT-16071) IDEA deadlock fixed: when typing "parse()" in .kt file
- [`KT-16149`](https://youtrack.jetbrains.com/issue/KT-16149) Intellij Idea 2017.1/Android Studio 2.3 beta3 and Kotlin plugin 1.1-beta2 deadlock fixed

### Coroutine libraries
- [`KT-15716`](https://youtrack.jetbrains.com/issue/KT-15716) Introduced startCoroutineUninterceptedOrReturn coroutine intrinsic
- [`KT-15718`](https://youtrack.jetbrains.com/issue/KT-15718) createCoroutine now returns safe continuation
- [`KT-16155`](https://youtrack.jetbrains.com/issue/KT-16155) Introduced createCoroutineUnchecked intrinsic


### Gradle support
- [`KT-15829`](https://youtrack.jetbrains.com/issue/KT-15829) Gradle Kotlin JS plugin: removed false "Duplicate source root:" warning for kotlin files
- [`KT-15902`](https://youtrack.jetbrains.com/issue/KT-15902) JS: gradle task output is now considered as source set output
- [`KT-16174`](https://youtrack.jetbrains.com/issue/KT-16174) Error fixed during IDEA-Gradle synchronization for Kotlin JS
- [`KT-16267`](https://youtrack.jetbrains.com/issue/KT-16267) JS: fixed regression in 1.1-beta2 for multi-module gradle project
- [`KT-16274`](https://youtrack.jetbrains.com/issue/KT-16274) Kotlin JS Gradle unexpected compiler error / absolute path to output file
- [`KT-16322`](https://youtrack.jetbrains.com/issue/KT-16322) Circlet project Gradle import issue fixed

### REPL
- [`KT-15861`](https://youtrack.jetbrains.com/issue/KT-15861) Use windows line separator in kotlin's JSR implementation
- [`KT-16126`](https://youtrack.jetbrains.com/issue/KT-16126) Proper `jvmTarget` for REPL compilation


## 1.1-Beta2

### Language related changes
- [`KT-7897`](https://youtrack.jetbrains.com/issue/KT-7897) Do not require to call enum constructor for each entry if all parameters have default values
- [`KT-8985`](https://youtrack.jetbrains.com/issue/KT-8985) Support T::class.java for T with no non-null upper bound
- [`KT-10711`](https://youtrack.jetbrains.com/issue/KT-10711) Type inference works now on generics for callable references
- [`KT-13130`](https://youtrack.jetbrains.com/issue/KT-13130) Support exhaustive when for sealed trees
- [`KT-15898`](https://youtrack.jetbrains.com/issue/KT-15898) Cannot use type alias to qualify enum entry
- [`KT-16061`](https://youtrack.jetbrains.com/issue/KT-16061) Smart type inference on callable references in 1.1 mode only

### Reflection
- [`KT-8384`](https://youtrack.jetbrains.com/issue/KT-8384) Access to the delegate object for a KProperty

### Compiler

#### Coroutine support
- [`KT-15016`](https://youtrack.jetbrains.com/issue/KT-15016) VerifyError with coroutine: fix processing of uninitialized instances
- [`KT-15527`](https://youtrack.jetbrains.com/issue/KT-15527) Coroutine compile error: wrong code generated for safe qualified suspension points
- [`KT-15552`](https://youtrack.jetbrains.com/issue/KT-15552) Accessor implementation of suspended function produces AbstractMethodError
- [`KT-15715`](https://youtrack.jetbrains.com/issue/KT-15715) Coroutine generate invalid invoke
- [`KT-15820`](https://youtrack.jetbrains.com/issue/KT-15820) Coroutine Internal Error regression with dispatcher + this@
- [`KT-15821`](https://youtrack.jetbrains.com/issue/KT-15821) Coroutine internal error regression: Could not inline method call apply
- [`KT-15824`](https://youtrack.jetbrains.com/issue/KT-15824) Coroutine iterator regression: Object cannot be cast to java.lang.Boolean
- [`KT-15827`](https://youtrack.jetbrains.com/issue/KT-15827) Show Kotlin Bytecode shows wrong bytecode for suspending functions
- [`KT-15907`](https://youtrack.jetbrains.com/issue/KT-15907) Bogus error about platform declaration clash with private suspend functions
- [`KT-15933`](https://youtrack.jetbrains.com/issue/KT-15933) Suspend getValue/setValue/provideDelegate do not work properly
- [`KT-15935`](https://youtrack.jetbrains.com/issue/KT-15935) Private suspend function in file causes UnsupportedOperationException: Context does not have a "this"
- [`KT-15963`](https://youtrack.jetbrains.com/issue/KT-15963) Coroutine: runtime error if returned object "equals" does not like comparison to SUSPENDED_MARKER
- [`KT-16068`](https://youtrack.jetbrains.com/issue/KT-16068) Prohibit inline lambda parameters of suspend function type

#### Diagnostics
- [`KT-1560`](https://youtrack.jetbrains.com/issue/KT-1560) Report diagnostic for a declaration of extension function which will be always shadowed by member function
- [`KT-12846`](https://youtrack.jetbrains.com/issue/KT-12846) Forbid vararg of Nothing
- [`KT-13227`](https://youtrack.jetbrains.com/issue/KT-13227) NO_ELSE_IN_WHEN in when by sealed class instance if is-check for base sealed class is used
- [`KT-13355`](https://youtrack.jetbrains.com/issue/KT-13355) Type mismatch on inheritance is not reported on abstract class
- [`KT-15010`](https://youtrack.jetbrains.com/issue/KT-15010) Missing error on an usage of non-constant property in annotation default argument 
- [`KT-15201`](https://youtrack.jetbrains.com/issue/KT-15201) Compiler is complaining about when statement without null condition even if null is checked before.
- [`KT-15736`](https://youtrack.jetbrains.com/issue/KT-15736) Report an error on type alias expanded to a nullable type on LHS of a class literal
- [`KT-15740`](https://youtrack.jetbrains.com/issue/KT-15740) Report error on expression of a nullable type on LHS of a class literal
- [`KT-15844`](https://youtrack.jetbrains.com/issue/KT-15844) Do not allow to access primary constructor parameters from property with custom getter
- [`KT-15878`](https://youtrack.jetbrains.com/issue/KT-15878) Extension shadowed by member should not be reported for infix/operator extensions when member is non-infix/operator
- [`KT-16010`](https://youtrack.jetbrains.com/issue/KT-16010) Do not highlight lambda parameters as unused in 1.0 compatibility mode

#### Kapt
- [`KT-15675`](https://youtrack.jetbrains.com/issue/KT-15675) Kapt3 does not generate classes annotated with AutoValue
- [`KT-15697`](https://youtrack.jetbrains.com/issue/KT-15697) If an annotation with AnnotationTarget.PROPERTY is tagged on a Kotlin property, it breaks annotation processing
- [`KT-15803`](https://youtrack.jetbrains.com/issue/KT-15803) Kotlin 1.0.6 broke Dagger
- [`KT-15814`](https://youtrack.jetbrains.com/issue/KT-15814) Regression: Kapt is not working in 1.0.6 / 1.1-M04 / 1.1-Beta
- [`KT-15838`](https://youtrack.jetbrains.com/issue/KT-15838) kapt3 1.1-beta: KaptError: Java file parsing error
- [`KT-15841`](https://youtrack.jetbrains.com/issue/KT-15841) 1.1-Beta + kapt3 fails to build the project with StackOverflowError
- [`KT-15915`](https://youtrack.jetbrains.com/issue/KT-15915) Kapt: Kotlin class target directory is cleared before compilation (and after kapt task)
- [`KT-16006`](https://youtrack.jetbrains.com/issue/KT-16006) Cannot determine if type is an error type during annotation processing

#### Exceptions / Errors
- [`KT-8264`](https://youtrack.jetbrains.com/issue/KT-8264) Internal compiler error: java.lang.ArithmeticException: BigInteger: modulus not positive
- [`KT-14547`](https://youtrack.jetbrains.com/issue/KT-14547) NoSuchElementException when compiling callable reference without stdlib in the classpath
- [`KT-14966`](https://youtrack.jetbrains.com/issue/KT-14966) Regression: VerifyError on access super implementation from delegate 
- [`KT-15017`](https://youtrack.jetbrains.com/issue/KT-15017) Throwing exception in the end of inline suspend-functions lead to internal compiler error
- [`KT-15439`](https://youtrack.jetbrains.com/issue/KT-15439) Resolved call is not completed for generic callable reference in if-expression
- [`KT-15500`](https://youtrack.jetbrains.com/issue/KT-15500) Exception passing freeCompilerArgs to gradle plugin
- [`KT-15646`](https://youtrack.jetbrains.com/issue/KT-15646) InconsistentDebugInfoException when stepping over `throw`
- [`KT-15726`](https://youtrack.jetbrains.com/issue/KT-15726) Kotlin compiles invalid bytecode for nested try-catch with return
- [`KT-15743`](https://youtrack.jetbrains.com/issue/KT-15743) Overloaded Kotlin extensions annotates wrong parameters in java
- [`KT-15868`](https://youtrack.jetbrains.com/issue/KT-15868) NPE when comparing nullable doubles for equality
- [`KT-15995`](https://youtrack.jetbrains.com/issue/KT-15995) Can't build project with DataBinding using Kotlin 1.1: incompatible language version
- [`KT-16047`](https://youtrack.jetbrains.com/issue/KT-16047) Internal Error: org.jetbrains.kotlin.util.KotlinFrontEndException while analyzing expression

#### Type inference issues
- [`KT-10268`](https://youtrack.jetbrains.com/issue/KT-10268) Wrong type inference related to captured types
- [`KT-11259`](https://youtrack.jetbrains.com/issue/KT-11259) Wrong type inference for Java 8 Stream.collect.
- [`KT-12802`](https://youtrack.jetbrains.com/issue/KT-12802) Type inference failed when irrelevant method reference is used
- [`KT-12964`](https://youtrack.jetbrains.com/issue/KT-12964) Support type inference for callable references from parameter types of an expected function type

#### Smart cast issues
- [`KT-13468`](https://youtrack.jetbrains.com/issue/KT-13468) Smart cast is broken after assignment of 'if' expression
- [`KT-14350`](https://youtrack.jetbrains.com/issue/KT-14350) Make smart-cast work as it does in 1.0 when -language-version 1.0 is used
- [`KT-14597`](https://youtrack.jetbrains.com/issue/KT-14597) When over smartcast enum is broken and breaks all other "when"
- [`KT-15792`](https://youtrack.jetbrains.com/issue/KT-15792) Wrong smart cast after y = x, x = null, y != null sequence

#### Various issues
- [`KT-15236`](https://youtrack.jetbrains.com/issue/KT-15236) False positive: Null can not be a value of a non-null type
- [`KT-15677`](https://youtrack.jetbrains.com/issue/KT-15677) Modifiers and annotations are lost on a (nullable) parenthesized type
- [`KT-15707`](https://youtrack.jetbrains.com/issue/KT-15707) IDEA unable to parallel compile different projects
- [`KT-15734`](https://youtrack.jetbrains.com/issue/KT-15734) Nullability is lost during expansion of a type alias
- [`KT-15748`](https://youtrack.jetbrains.com/issue/KT-15748) Type alias constructor return type should have a corresponding abbreviation
- [`KT-15775`](https://youtrack.jetbrains.com/issue/KT-15775) Annotations are lost on value parameter types of a function type
- [`KT-15780`](https://youtrack.jetbrains.com/issue/KT-15780) Treat Map.getOrDefault overrides in Java the same way as in 1.0.x compiler with language version 1.0
- [`KT-15794`](https://youtrack.jetbrains.com/issue/KT-15794) Refine backward compatibility mode for additional built-ins members from JDK
- [`KT-15848`](https://youtrack.jetbrains.com/issue/KT-15848) Implement additional annotation processing in the `KotlinScriptDefinitionFromAnnotatedTemplate` for SamWithReceiver plugin
- [`KT-15875`](https://youtrack.jetbrains.com/issue/KT-15875) Operation has lead to overflow for 'mod' with negative first operand
- [`KT-15945`](https://youtrack.jetbrains.com/issue/KT-15945) Feature Request: Andrey Breslav to grow a beard.

### JavaScript backend

#### Coroutine support
- [`KT-15834`](https://youtrack.jetbrains.com/issue/KT-15834) JS: Local delegate in suspend function
- [`KT-15892`](https://youtrack.jetbrains.com/issue/KT-15892) JS: safe call of suspend functions causes compiler to crash

#### Diagnostics
- [`KT-14668`](https://youtrack.jetbrains.com/issue/KT-14668) Do not allow declarations in 'kotlin' package or subpackages in JS
- [`KT-15184`](https://youtrack.jetbrains.com/issue/KT-15184) JS: prohibit `..` operation with `dynamic` on left-hand side
- [`KT-15253`](https://youtrack.jetbrains.com/issue/KT-15253) JS: no error when use class external class with JsModule in type context when compiling with plain module kind
- [`KT-15283`](https://youtrack.jetbrains.com/issue/KT-15283) JS: additional restrictions on dynamic
- [`KT-15961`](https://youtrack.jetbrains.com/issue/KT-15961) Could not implement external open class with function with optional parameter

#### Language feature support
- [`KT-14035`](https://youtrack.jetbrains.com/issue/KT-14035) JS: support implementing CharSequence
- [`KT-14036`](https://youtrack.jetbrains.com/issue/KT-14036) JS: use Int16 for Char when it possible and box to our Char otherwise
- [`KT-14097`](https://youtrack.jetbrains.com/issue/KT-14097) Wrong code generated for enum entry initialization using non-primary no-argument constructor
- [`KT-15312`](https://youtrack.jetbrains.com/issue/KT-15312) JS: map kotlin.Throwable to JS Error
- [`KT-15765`](https://youtrack.jetbrains.com/issue/KT-15765) JS: support callable references on built-in and intrinsic functions and properties
- [`KT-15900`](https://youtrack.jetbrains.com/issue/KT-15900) JS: Support enum entry with empty initializer with vararg constructor

#### Standard library support
- [`KT-4141`](https://youtrack.jetbrains.com/issue/KT-4141) JS: wrong return type for Date::getTime
- [`KT-4497`](https://youtrack.jetbrains.com/issue/KT-4497) JS: add String.toInt, String.toDouble etc extension functions, `parseInt` and `parseFloat` are deprecated in favor of these new ones
- [`KT-15940`](https://youtrack.jetbrains.com/issue/KT-15940) JS: rename all js standard library artifacts (both in maven and in compiler distribution) to `kotlin-stdlib-js.jar`
- Add `Promise<T>` external declaration to the standard library
- Types like `Date`, `Math`, `Console`, `Promise`, `RegExp`, `Json` require explicit import from `kotlin.js` package

#### External declarations
- [`KT-15144`](https://youtrack.jetbrains.com/issue/KT-15144) JS: rename `noImpl` to `definedExternally`
- [`KT-15306`](https://youtrack.jetbrains.com/issue/KT-15306) JS: allow to use `definedExternally` only inside a body of external declarations
- [`KT-15336`](https://youtrack.jetbrains.com/issue/KT-15336) JS: allow to inherit external classes from kotlin.Throwable
- [`KT-15905`](https://youtrack.jetbrains.com/issue/KT-15905) JS: add a way to control qualifier for external declarations inside file
- Deprecate `@native` annotation, to be removed in 1.1 release.

#### Exceptions / Errors
- [`KT-10894`](https://youtrack.jetbrains.com/issue/KT-10894) Infinite indexing at projects with JS modules
- [`KT-14124`](https://youtrack.jetbrains.com/issue/KT-14124) AssertionError: strings file not found on K2JS serialized data
 
#### Various issues
- [`KT-8211`](https://youtrack.jetbrains.com/issue/KT-8211) JS: generate dummy init for properties w/o initializer to avoid to have different hidden classes for different instances
- [`KT-12712`](https://youtrack.jetbrains.com/issue/KT-12712) JS: Json should not be a class
- [`KT-13312`](https://youtrack.jetbrains.com/issue/KT-13312) JS: can't use extension lambda where expected lambda and vice versa
- [`KT-13632`](https://youtrack.jetbrains.com/issue/KT-13632) Add template kotlin js project under gradle in "New Project" window
- [`KT-15278`](https://youtrack.jetbrains.com/issue/KT-15278) JS: don't treat property access through dynamic as side effect free
- [`KT-15285`](https://youtrack.jetbrains.com/issue/KT-15285) JS: take into account as many characteristics from the signature as possible when mangling
- [`KT-15678`](https://youtrack.jetbrains.com/issue/KT-15678) JS: Generated local variable named 'element' clashes with actual local variable named 'element'
- [`KT-15755`](https://youtrack.jetbrains.com/issue/KT-15755) JS compiler produces a lot of empty kotlin_file_table files for irrelevant packages
- [`KT-15770`](https://youtrack.jetbrains.com/issue/KT-15770) Name clash between recursive local functions with same name
- [`KT-15797`](https://youtrack.jetbrains.com/issue/KT-15797) JS: wrong code for accessing nested class inside js module
- [`KT-15863`](https://youtrack.jetbrains.com/issue/KT-15863) JS: Extension function reference shifts parameters loosing the receiver
- [`KT-16049`](https://youtrack.jetbrains.com/issue/KT-16049) JS: drop "-kjsm" command line option, merge the logic into "-meta-info"
- [`KT-16083`](https://youtrack.jetbrains.com/issue/KT-16083) JS: rename "-library-files" argument to "-libraries" and change separator from comma to system file separator

### Standard Library
- [`KT-13353`](https://youtrack.jetbrains.com/issue/KT-13353) Add Map.minus(key) and Map.minus(keys) 
- [`KT-13826`](https://youtrack.jetbrains.com/issue/KT-13826) Add parameter names in function types used in the standard library
- [`KT-14279`](https://youtrack.jetbrains.com/issue/KT-14279) Make String.matches(Regex) and Regex.matches(String) infix
- [`KT-15399`](https://youtrack.jetbrains.com/issue/KT-15399) Iterable.average() now returns NaN for an empty collection
- [`KT-15975`](https://youtrack.jetbrains.com/issue/KT-15975) Move coroutine-related runtime parts to `kotlin.coroutines.experimental` package
- [`KT-16030`](https://youtrack.jetbrains.com/issue/KT-16030) Move bitwise operations on Byte and Short to `kotlin.experimental` package
- [`KT-16026`](https://youtrack.jetbrains.com/issue/KT-16026) Classes compiled in 1.1 in 1.0-compatibility mode may contain references to CloseableKt class from 1.1

### IDE

#### Configuration issues
- [`KT-15621`](https://youtrack.jetbrains.com/issue/KT-15621) Copy compiler options values from project settings on creating a kotlin facet for Kotlin (JVM) project
- [`KT-15623`](https://youtrack.jetbrains.com/issue/KT-15623) Copy compiler options values from project settings on creating a kotlin facet for Kotlin (JavaScript) project
- [`KT-15624`](https://youtrack.jetbrains.com/issue/KT-15624) Set option "Use project settings" in newly created Kotlin facet
- [`KT-15712`](https://youtrack.jetbrains.com/issue/KT-15712) Configuring a project with Maven or Gradle should automatically use stdlib-jre7 or stdlib-jre8 instead of standard stdlib
- [`KT-15772`](https://youtrack.jetbrains.com/issue/KT-15772) Facet does not pick up api version from maven
- [`KT-15819`](https://youtrack.jetbrains.com/issue/KT-15819) It would be nice if compileKotlin options are imported into Kotlin facet from gradle/maven
- [`KT-16015`](https://youtrack.jetbrains.com/issue/KT-16015) Prohibit api-version > language-version in Facet and Project Settings

#### Coroutine support
- [`KT-14704`](https://youtrack.jetbrains.com/issue/KT-14704) Extract Method should work in coroutines
- [`KT-15955`](https://youtrack.jetbrains.com/issue/KT-15955) Quick-fix to enable coroutines through Gradle project configuration
- [`KT-16018`](https://youtrack.jetbrains.com/issue/KT-16018) Hide coroutines intrinsics from import and completion
- [`KT-16075`](https://youtrack.jetbrains.com/issue/KT-16075) Error:Kotlin: The -Xcoroutines can only have one value

#### Backward compatibility issues
- [`KT-15134`](https://youtrack.jetbrains.com/issue/KT-15134) Do not suggest using destructuring lambda if this will result in "available since 1.1" error
- [`KT-15918`](https://youtrack.jetbrains.com/issue/KT-15918) Quick fix "Set module language level to 1.1" should also set API version to 1.1
- [`KT-15969`](https://youtrack.jetbrains.com/issue/KT-15969) Replace operator with function should use either rem or mod for % depending on language version
- [`KT-15978`](https://youtrack.jetbrains.com/issue/KT-15978) Type alias from Kotlin 1.1 are suggested in completion even if language level is set to 1.0 in settings
- [`KT-15979`](https://youtrack.jetbrains.com/issue/KT-15979) Usages of type aliases are not shown as errors in editor if language version is set to 1.0
- [`KT-16019`](https://youtrack.jetbrains.com/issue/KT-16019) Do not suggest renaming to underscore in 1.0 compatibility mode
- [`KT-16036`](https://youtrack.jetbrains.com/issue/KT-16036) "Create type alias from usage" quick-fix should not be suggested at language level 1.0

#### Intention actions, inspections and quick-fixes

##### New features
- [`KT-9912`](https://youtrack.jetbrains.com/issue/KT-9912) Merge ifs intention
- [`KT-13427`](https://youtrack.jetbrains.com/issue/KT-13427) "Specify type explicitly" should support type aliases
- [`KT-15066`](https://youtrack.jetbrains.com/issue/KT-15066) "Make private/.." intention on type aliases
- [`KT-15709`](https://youtrack.jetbrains.com/issue/KT-15709) Add inspection for private primary constructors in data classes as they are accessible via the copy method
- [`KT-15738`](https://youtrack.jetbrains.com/issue/KT-15738) Intention to add `suspend` modifier to functional type
- [`KT-15800`](https://youtrack.jetbrains.com/issue/KT-15800) Quick-fix to convert a function to suspending on error when calling suspension inside

##### Bug fixes
- [`KT-13710`](https://youtrack.jetbrains.com/issue/KT-13710) Import intention action should not appear in import list
- [`KT-14680`](https://youtrack.jetbrains.com/issue/KT-14680) import statement to type alias reported as unused when using only TA constructor
- [`KT-14856`](https://youtrack.jetbrains.com/issue/KT-14856) TextView internationalisation intention does not report the problem
- [`KT-14993`](https://youtrack.jetbrains.com/issue/KT-14993) Keep destructuring declaration parameter on inspection "Remove explicit lambda parameter types"
- [`KT-14994`](https://youtrack.jetbrains.com/issue/KT-14994) PsiInvalidElementAccessException and incorrect generation on inspection "Specify type explicitly" on destructuring parameter
- [`KT-15162`](https://youtrack.jetbrains.com/issue/KT-15162) "Remove explicit lambda parameter types" intentions fails with destructuring declaration with KNPE at KtPsiFactory.createLambdaParameterList()
- [`KT-15311`](https://youtrack.jetbrains.com/issue/KT-15311) "Add Import" intention generates incorrect code
- [`KT-15406`](https://youtrack.jetbrains.com/issue/KT-15406) Convert to secondary constructor for enum class should put new members after enum values
- [`KT-15553`](https://youtrack.jetbrains.com/issue/KT-15553) Copy concatenation text to clipboard with Kotlin and string interpolation does not work
- [`KT-15670`](https://youtrack.jetbrains.com/issue/KT-15670) 'Convert to lambda' quick fix in IDEA leaves single-line comment and } gets commented out
- [`KT-15873`](https://youtrack.jetbrains.com/issue/KT-15873) Alt+Enter menu isn't shown for deprecated mod function
- [`KT-15874`](https://youtrack.jetbrains.com/issue/KT-15874) Replace operator with function call replaces % with deprecated mod
- [`KT-15884`](https://youtrack.jetbrains.com/issue/KT-15884) False positive "Redundant .let call"
- [`KT-16072`](https://youtrack.jetbrains.com/issue/KT-16072) Intentions to convert suspend lambdas to callable references should not be shown

#### Android support
- [`KT-13275`](https://youtrack.jetbrains.com/issue/KT-13275) Kotlin Gradle plugin for Android does not work when jackOptions enabled
- [`KT-15150`](https://youtrack.jetbrains.com/issue/KT-15150) Android: Add quick-fix to generate View constructor convention
- [`KT-15282`](https://youtrack.jetbrains.com/issue/KT-15282) Issues debugging crossinline Android code

#### KDoc
- [`KT-14710`](https://youtrack.jetbrains.com/issue/KT-14710) Sample references are not resolved in IDE
- [`KT-15796`](https://youtrack.jetbrains.com/issue/KT-15796) Import of class referenced only in KDoc not preserved after copy-paste

#### Various issues
- [`KT-9011`](https://youtrack.jetbrains.com/issue/KT-9011) Shift+Enter should insert curly braces when invoked after class declaration
- [`KT-11308`](https://youtrack.jetbrains.com/issue/KT-11308) Hide kotlin.jvm.internal package contents from completion and auto-import
- [`KT-14252`](https://youtrack.jetbrains.com/issue/KT-14252) Completion could suggest constructors available via type aliases
- [`KT-14722`](https://youtrack.jetbrains.com/issue/KT-14722) Completion list isn't filled up for type alias to object
- [`KT-14767`](https://youtrack.jetbrains.com/issue/KT-14767) Type alias to annotation class should appear in the completion list
- [`KT-14859`](https://youtrack.jetbrains.com/issue/KT-14859) "Parameter Info" sometimes does not work properly inside lambda
- [`KT-15032`](https://youtrack.jetbrains.com/issue/KT-15032) Injected fragment: descriptor was not found for declaration: FUN
- [`KT-15153`](https://youtrack.jetbrains.com/issue/KT-15153) Support typeAlias extensions in completion and add import
- [`KT-15786`](https://youtrack.jetbrains.com/issue/KT-15786) NoSuchMethodError: com.intellij.util.containers.UtilKt.isNullOrEmpty
- [`KT-15883`](https://youtrack.jetbrains.com/issue/KT-15883) Generating equals() and hashCode(): hashCode does not correctly honor variable names with back ticks
- [`KT-15911`](https://youtrack.jetbrains.com/issue/KT-15911) Kotlin REPL will not launch: "Neither main class nor JAR path is specified"

### J2K
- [`KT-15789`](https://youtrack.jetbrains.com/issue/KT-15789) Kotlin plugin incorrectly converts for-loops from Java to Kotlin

### Gradle support
- [`KT-14830`](https://youtrack.jetbrains.com/issue/KT-14830) Kotlin Gradle plugin configuration should not add 'kotlin' source directory by default
- [`KT-15279`](https://youtrack.jetbrains.com/issue/KT-15279) 'Kotlin not configured message' should not be displayed while gradle sync is in progress
- [`KT-15812`](https://youtrack.jetbrains.com/issue/KT-15812) Create Kotlin facet on importing gradle project with unchecked option Create separate module per source set
- [`KT-15837`](https://youtrack.jetbrains.com/issue/KT-15837) Gradle compiler attempts to connect to daemon on address derived from DNS lookup
- [`KT-15909`](https://youtrack.jetbrains.com/issue/KT-15909) Copy Gradle compiler options to facets in Intellij/AS
- [`KT-15929`](https://youtrack.jetbrains.com/issue/KT-15929) Gradle project imported with wrong 'target platform'

### Other issues
- [`KT-15450`](https://youtrack.jetbrains.com/issue/KT-15450) JSR 223 - support eval with bindings


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


## 1.1-M04 (EAP-4)

### Language related changes

- [`KT-4481`](https://youtrack.jetbrains.com/issue/KT-4481) compareTo on primitive floats/doubles should behave naturally
- [`KT-11016`](https://youtrack.jetbrains.com/issue/KT-11016) Allow to annotate internal API to be used inside public inline functions
- [`KT-11128`](https://youtrack.jetbrains.com/issue/KT-11128) Member vs SAM conversion with more specific signature
- [`KT-12215`](https://youtrack.jetbrains.com/issue/KT-12215) Allowing to access protected members in public inline members creates potential binary compatibility problem
- [`KT-12531`](https://youtrack.jetbrains.com/issue/KT-12531) Report error when delegated member hides a supertype member
- [`KT-14650`](https://youtrack.jetbrains.com/issue/KT-14650) mod function on integral types is inconsistent with BigInteger.mod
- [`KT-14651`](https://youtrack.jetbrains.com/issue/KT-14651) Floating point comparisons shall operate according to IEEE754
- [`KT-14852`](https://youtrack.jetbrains.com/issue/KT-14852) It should not be possible to use typealias that abbreviates a generic projection as a constructor
- [`KT-15226`](https://youtrack.jetbrains.com/issue/KT-15226) Restrict delegation to java 8 default methods

### Reflection

- [`KT-12250`](https://youtrack.jetbrains.com/issue/KT-12250) Provide API for getting a single annotation by its class
- [`KT-14939`](https://youtrack.jetbrains.com/issue/KT-14939) VerifyError in accessors for bound property reference with receiver 'null'

### Compiler

#### Coroutines

- Major coroutines redesign - see [`KEEP`](https://github.com/Kotlin/kotlin-coroutines/blob/master/kotlin-coroutines-informal.md) for details

#### Optimizations

- [`KT-11734`](https://youtrack.jetbrains.com/issue/KT-11734) Optimize const vals by inlining them at call site
- [`KT-13570`](https://youtrack.jetbrains.com/issue/KT-13570) Generate TABLE/LOOKUPSWITCH if all when branches are const integer values
- [`KT-14746`](https://youtrack.jetbrains.com/issue/KT-14746) Captured Refs should not be volatile

#### Various issues

- [`KT-10982`](https://youtrack.jetbrains.com/issue/KT-10982) java.util.Map::compute* poor usability
- [`KT-12144`](https://youtrack.jetbrains.com/issue/KT-12144) Type inference incorporation error on SAM adapter call
- [`KT-14196`](https://youtrack.jetbrains.com/issue/KT-14196) Do not allow class literal with expression in annotation arguments
- [`KT-14453`](https://youtrack.jetbrains.com/issue/KT-14453) Regression: Type inference failed: inferred type is T but T was expected
- [`KT-14774`](https://youtrack.jetbrains.com/issue/KT-14774) Incorrect inner class modifier generated for sealed inner classes
- [`KT-14839`](https://youtrack.jetbrains.com/issue/KT-14839) CompilationException when calling inline fun with first arg of 2 (w/defaults) within catch block of Java exception type
- [`KT-14855`](https://youtrack.jetbrains.com/issue/KT-14855) Projection in type aliases should be allowed in supertypes and constructor invocations if they expand to non-toplevel projections
- [`KT-14887`](https://youtrack.jetbrains.com/issue/KT-14887) Unhelpful error "public-API inline function cannot access non-public-API" for unresolved call inside inline function
- [`KT-14930`](https://youtrack.jetbrains.com/issue/KT-14930) Android: creating Kotlin activity: UOE at EmptyList.removeAll()
- [`KT-15146`](https://youtrack.jetbrains.com/issue/KT-15146) Kapt3 no source files on unittest
- [`KT-15272`](https://youtrack.jetbrains.com/issue/KT-15272) Exception when building 2 projects at the same time

### JavaScript backend

#### dynamic type

- [`KT-8207`](https://youtrack.jetbrains.com/issue/KT-8207) Extension function on dynamic resolves on any type
- [`KT-6579`](https://youtrack.jetbrains.com/issue/KT-6579) JS: prohibit to use `in` and `!in` on dynamic
- [`KT-6580`](https://youtrack.jetbrains.com/issue/KT-6580) JS: prohibit to use more than one argument in indexed access on dynamic
- [`KT-13615`](https://youtrack.jetbrains.com/issue/KT-13615) JS: don't generate guard for catch with dynamic type

#### @native/external

- [`KT-13893`](https://youtrack.jetbrains.com/issue/KT-13893) JS: Replace @native annotation with external modifier
- [`KT-12877`](https://youtrack.jetbrains.com/issue/KT-12877) Allow to specify module for native JS declarations
- [`KT-14806`](https://youtrack.jetbrains.com/issue/KT-14806) JS: name of a local variable clashes with native declaration from global scope

#### Diagnostics

- [`KT-13889`](https://youtrack.jetbrains.com/issue/KT-13889) JS: prohibit overriding native functions with default values assigned to parameters
- [`KT-13894`](https://youtrack.jetbrains.com/issue/KT-13894) JS: prohibit native declaration inside non-native
- [`KT-13895`](https://youtrack.jetbrains.com/issue/KT-13895) JS: RUNTIME annotations
- [`KT-13896`](https://youtrack.jetbrains.com/issue/KT-13896) JS: prohibit external(native) extension functions and properties
- [`KT-13897`](https://youtrack.jetbrains.com/issue/KT-13897) JS: prohibit native(external) files and typealiases
- [`KT-13910`](https://youtrack.jetbrains.com/issue/KT-13910) JS: prohibit override members of native declaration with overloads
- [`KT-14027`](https://youtrack.jetbrains.com/issue/KT-14027) JS: prohibit native inner classes
- [`KT-14029`](https://youtrack.jetbrains.com/issue/KT-14029) JS: prohibit private members inside native declarations
- [`KT-14037`](https://youtrack.jetbrains.com/issue/KT-14037) JS: prohibit using native interfaces in RHS of IS
- [`KT-14038`](https://youtrack.jetbrains.com/issue/KT-14038) JS: warn when using native interface in RHS of AS
- [`KT-15130`](https://youtrack.jetbrains.com/issue/KT-15130) JS: prohibit inheritance native from non-native
- [`KT-12600`](https://youtrack.jetbrains.com/issue/KT-12600) JS: type check with a native interface compiles but crash at runtime
- [`KT-13307`](https://youtrack.jetbrains.com/issue/KT-13307) KotlinJS cannot cast to a marker interface.

#### Language features support

- [`KT-13573`](https://youtrack.jetbrains.com/issue/KT-13573) JS: support bound callable reference
- [`KT-14634`](https://youtrack.jetbrains.com/issue/KT-14634) JS: support enumValues / enumValueOf
- [`KT-15058`](https://youtrack.jetbrains.com/issue/KT-15058) JS: replace suspend function convention

#### Issues related to kotlin.Any

- [`KT-7664`](https://youtrack.jetbrains.com/issue/KT-7664) JS: "x is Any" is always false
- [`KT-7665`](https://youtrack.jetbrains.com/issue/KT-7665) JS: creating Any instance crashes on runtime
- [`KT-15131`](https://youtrack.jetbrains.com/issue/KT-15131) JS: don't mangle Any.equals

#### Various issues

- [`KT-14033`](https://youtrack.jetbrains.com/issue/KT-14033) JS: don't optimize (based on type information) by default expressions with any of "as, is, !is, as?, ?., !!"
- [`KT-13616`](https://youtrack.jetbrains.com/issue/KT-13616) JS: don't omit guard for catch with Throwable type
- [`KT-12976`](https://youtrack.jetbrains.com/issue/KT-12976) JS: human-friendly error message on wrong modules order
- [`KT-15212`](https://youtrack.jetbrains.com/issue/KT-15212) JS: link unqualified names in `js(...)` function to local functions in outer Kotlin function by name
- [`KT-14750`](https://youtrack.jetbrains.com/issue/KT-14750) JS: remove unnecessary functions from kotlin.js

#### Bugfixes

- [`KT-12566`](https://youtrack.jetbrains.com/issue/KT-12566) JS: inner local class should refer to captured variables via its outer class
- [`KT-12527`](https://youtrack.jetbrains.com/issue/KT-12527) Reified is-check works wrongly for chained calls
- [`KT-12586`](https://youtrack.jetbrains.com/issue/KT-12586) JS: compiler crashes when call inline function inside string templeate
- [`KT-13164`](https://youtrack.jetbrains.com/issue/KT-13164) Ecma TypeError on extending local class from inner one
- [`KT-14888`](https://youtrack.jetbrains.com/issue/KT-14888) JS: Compiler error: Cannot get FQ name of local class: lazy class <no name provided>
- [`KT-14748`](https://youtrack.jetbrains.com/issue/KT-14748) JS: eliminate unused functions
- [`KT-14999`](https://youtrack.jetbrains.com/issue/KT-14999) JS: Operator set + labeled lambdas
- [`KT-15007`](https://youtrack.jetbrains.com/issue/KT-15007) JS: Dies when checking if exception implements interface. TypeError: Cannot read property 'baseClasses' of undefined
- [`KT-15073`](https://youtrack.jetbrains.com/issue/KT-15073) KT to JS losing extension function's receiver
- [`KT-15169`](https://youtrack.jetbrains.com/issue/KT-15169) JS: compiler fails on annotated expression with TRE at Translation.doTranslateExpression()
- [`KT-13522`](https://youtrack.jetbrains.com/issue/KT-13522) JS: can't use captured reified type paramter in jsClass
- [`KT-13784`](https://youtrack.jetbrains.com/issue/KT-13784) JS: lambda was not inlined for function with reified parameter declared in another module
- [`KT-13792`](https://youtrack.jetbrains.com/issue/KT-13792) JS: inner class of local class does not capture enclosing class properly
- [`KT-15327`](https://youtrack.jetbrains.com/issue/KT-15327) JS: Enum `valueOf` should throw IllegalArgumentException

### Standard library

- [`KT-7930`](https://youtrack.jetbrains.com/issue/KT-7930) Make String.toInt(), toLong(), etc. nullable instead of throwing exception
- [`KT-8220`](https://youtrack.jetbrains.com/issue/KT-8220) Add #peek method to Sequence similar to Stream.peek
- [`KT-8286`](https://youtrack.jetbrains.com/issue/KT-8286) Int.toString and String.toInt with base as parameter
- [`KT-14034`](https://youtrack.jetbrains.com/issue/KT-14034) JS: unsafeCast function
- [`KT-15181`](https://youtrack.jetbrains.com/issue/KT-15181) Some source files are missing from published sources on Bintray

### IDE

- [`KT-15205`](https://youtrack.jetbrains.com/issue/KT-15205) Implement quick-fix for increasing module language level to enable unsupported language features

###### Issues fixed
- [`KT-14693`](https://youtrack.jetbrains.com/issue/KT-14693) Introduce Type Alias: Do not suggest type qualifiers
- [`KT-14696`](https://youtrack.jetbrains.com/issue/KT-14696) Introduce Type Alias: Fix NPE during dialog repaint
- [`KT-14685`](https://youtrack.jetbrains.com/issue/KT-14685) Introduce Type Alias: Replace type usages in constructor calls
- [`KT-14861`](https://youtrack.jetbrains.com/issue/KT-14861) Introduce Type Alias: Support callable references/class literals
- [`KT-15204`](https://youtrack.jetbrains.com/issue/KT-15204) Implement navigation from header to its implementation and vice versa
- [`KT-15269`](https://youtrack.jetbrains.com/issue/KT-15269) Quickfix for external (native) extension declarations
- [`KT-15293`](https://youtrack.jetbrains.com/issue/KT-15293) Add 1.1 EAP repository when creating a new Gradle project with 1.1 EAP

### Scripting

- [`KT-14538`](https://youtrack.jetbrains.com/issue/KT-14538) Kotlin gradle script files appear totally unresolved
- [`KT-14706`](https://youtrack.jetbrains.com/issue/KT-14706) Support package declaration in scripting
- [`KT-14707`](https://youtrack.jetbrains.com/issue/KT-14707) Support javax.script.Invocable on the JSR 223 ScriptEngine
- [`KT-14708`](https://youtrack.jetbrains.com/issue/KT-14708) kotlin-script-runtime is not published
- [`KT-14713`](https://youtrack.jetbrains.com/issue/KT-14713) Make it possible to use JSR 223 support without specifying compiler JAR absolute path
- [`KT-15064`](https://youtrack.jetbrains.com/issue/KT-15064) Gradle build with script .kts file: NPE at ScriptCodegen.genConstructor()

### Gradle support

- [`KT-15080`](https://youtrack.jetbrains.com/issue/KT-15080) Gradle build fails with Gradle 3.2 (master)
- [`KT-15120`](https://youtrack.jetbrains.com/issue/KT-15120) Gradle JS test compile task doesn't pick up production code
- [`KT-15127`](https://youtrack.jetbrains.com/issue/KT-15127) JS "compiler jar not found" with Gradle 3.2
- [`KT-15133`](https://youtrack.jetbrains.com/issue/KT-15133) Recent gradle-script-kotlin 3.3 distributions are unusable
- [`KT-15218`](https://youtrack.jetbrains.com/issue/KT-15218) Isolate Gradle Kotlin compiler process


## 1.1-M03 (EAP-3)

### New language features

- [`KT-2964`](https://youtrack.jetbrains.com/issue/KT-2964) Underscores in integer literals
    (see [KEEP](https://github.com/Kotlin/KEEP/blob/master/proposals/underscores-in-numeric-literals.md))
- [`KT-3824`](https://youtrack.jetbrains.com/issue/KT-3824) Underscore in lambda for unused parameters
    (see [KEEP](https://github.com/Kotlin/KEEP/blob/master/proposals/underscore-for-unused-parameters.md))
- [`KT-2783`](https://youtrack.jetbrains.com/issue/KT-2783) Allow to skip some components in a multi-declaration
    (see the same [KEEP](https://github.com/Kotlin/KEEP/blob/master/proposals/underscore-for-unused-parameters.md))
- [`KT-11551`](https://youtrack.jetbrains.com/issue/KT-11551) limited scope for dsl writers 
    (see [KEEP](https://github.com/Kotlin/KEEP/blob/master/proposals/scope-control-for-implicit-receivers.md))

### Compiler

#### Coroutines related issues
- Make fields for storing lambda parameters non-final (as they get assigned within `invoke` call)
- [`KT-14719`](https://youtrack.jetbrains.com/issue/KT-14719) Make initial continuation able to be resumed with exception 
- [`KT-14636`](https://youtrack.jetbrains.com/issue/KT-14636) Coroutine fields should not be volatile
- [`KT-14718`](https://youtrack.jetbrains.com/issue/KT-14718) Validate label value of coroutine in case of no suspension points

#### Typealises related issues
- [`KT-13514`](https://youtrack.jetbrains.com/issue/KT-13514) Type inference doesn't work with generic typealiases
- [`KT-13837`](https://youtrack.jetbrains.com/issue/KT-13837) Error "Type alias expands to T, which is not a class, an interface, or an object" 
    should also appear for local type aliases
- [`KT-14307`](https://youtrack.jetbrains.com/issue/KT-14307) Local recursive type alias should be an error
- [`KT-14400`](https://youtrack.jetbrains.com/issue/KT-14400) Compiler Error IllegalStateException: kotlin.NotImplementedError when anonymous 
    object inherits from typealias
- [`KT-14377`](https://youtrack.jetbrains.com/issue/KT-14377) Expected error: Modifier 'companion' is not applicable to 'typealias'
- [`KT-14498`](https://youtrack.jetbrains.com/issue/KT-14498) typealias allows to circumvent variance annotations
- [`KT-14641`](https://youtrack.jetbrains.com/issue/KT-14641) An exception while processing a nested type alias access after a dot

#### Various issues
- [`KT-550`](https://youtrack.jetbrains.com/issue/KT-550) Properties without initializer but with get must infer type from getter
- [`KT-8816`](https://youtrack.jetbrains.com/issue/KT-8816) Generate Kotlin parameter names in the same form as expected for Java 8 reflection
- [`KT-10569`](https://youtrack.jetbrains.com/issue/KT-10569) Cannot iterate over values of an enum class when it is used as a generic parameter
    (see [KEEP](https://github.com/Kotlin/KEEP/blob/master/proposals/generic-values-and-valueof-for-enums.md))
- [`KT-13557`](https://youtrack.jetbrains.com/issue/KT-13557) VerifyError with delegated local variable used in object expression
- [`KT-13890`](https://youtrack.jetbrains.com/issue/KT-13890) IllegalAccessError when invoking protected method with default arguments
- [`KT-14012`](https://youtrack.jetbrains.com/issue/KT-14012) Back-end (JVM) Internal error every first compilation after the source code change
- [`KT-14201`](https://youtrack.jetbrains.com/issue/KT-14201) UnsupportedOperationException: Don't know how to generate outer expression for anonymous 
    object with invoke and non-trivial closure
- [`KT-14318`](https://youtrack.jetbrains.com/issue/KT-14318) Repeated annotations resulting from type alias expansion should be reported
- [`KT-14347`](https://youtrack.jetbrains.com/issue/KT-14347) Report UNUSED_PARAMETER/VARIABLE on named unused lambda parameters/destructuring entries
- [`KT-14352`](https://youtrack.jetbrains.com/issue/KT-14352) @SinceKotlin is not taken into account for companion object member referenced via 
    type alias
- [`KT-14357`](https://youtrack.jetbrains.com/issue/KT-14357) Try-catch used in false condition generates CompilationException
- [`KT-14502`](https://youtrack.jetbrains.com/issue/KT-14502) Prohibit irrelevant modifiers and annotations on destructured parameters in lambda
- [`KT-14692`](https://youtrack.jetbrains.com/issue/KT-14692) Change resolution scope for componentX in lambda parameters
- [`KT-14824`](https://youtrack.jetbrains.com/issue/KT-14824) Back-end (JVM) Internal error: Couldn't inline method call 'get' into local final fun 
    StorageComponentContainer.<anonymous>(): kotlin.Unit
- [`KT-14798`](https://youtrack.jetbrains.com/issue/KT-14798) Gradle 3.2 AssertionError: Built-in class kotlin.ParameterName is not found

### JS

#### Feature support
- [`KT-6985`](https://youtrack.jetbrains.com/issue/KT-6985) Support Exceptions in JS
- [`KT-13574`](https://youtrack.jetbrains.com/issue/KT-13574) JS: support coroutines
- [`KT-14422`](https://youtrack.jetbrains.com/issue/KT-14422) JS: Support destructuring in lambda parameters
- [`KT-14507`](https://youtrack.jetbrains.com/issue/KT-14507) JS: allow to skip some components in a multi-declaration

#### Library updates
- [`KT-14637`](https://youtrack.jetbrains.com/issue/KT-14637) JS: Missing ArrayList.ensureCapacity
    
#### Other issues
- [`KT-2328`](https://youtrack.jetbrains.com/issue/KT-2328) js: kotlin exceptions must inherit Error
- [`KT-5537`](https://youtrack.jetbrains.com/issue/KT-5537) Drop Cloneable in JS
- [`KT-7014`](https://youtrack.jetbrains.com/issue/KT-7014) JS: generate code which more friendly to js tools (minifier, optimizer, linter etc)
- [`KT-8019`](https://youtrack.jetbrains.com/issue/KT-8019) JS: no stackTrace in exception subclasses
- [`KT-10911`](https://youtrack.jetbrains.com/issue/KT-10911) JS: Throwable properties aren't supported well
- [`KT-13912`](https://youtrack.jetbrains.com/issue/KT-13912) JS: Compiler NPE at JsSourceGenerationVisitor. Lambda with empty [if] block passed 
    to inline function
- [`KT-14535`](https://youtrack.jetbrains.com/issue/KT-14535) JS: Broken modification of captured variables defined by a destructuring declaration

### Standard Library
- [`KT-2084`](https://youtrack.jetbrains.com/issue/KT-2084) Common API should be available without referring to java.* packages

    Now those common types, which are supported on all platforms, are available in `kotlin.*` packages, and are imported by default. These include:
    - `ArrayList`, `HashSet`, `LinkedHashSet`, `HashMap`, `LinkedHashMap` in `kotlin.collections`
    - `Appendable` and `StringBuilder` in `kotlin.text`
    - `Comparator` in `kotlin.comparisons`
    On JVM these are just typealiases of the good old types from `java.util` and `java.lang`
- [`KT-13554`](https://youtrack.jetbrains.com/issue/KT-13554) Introduce bitwise operations `and`/`or`/`xor`/`inv` for Byte and Short
- [`KT-13582`](https://youtrack.jetbrains.com/issue/KT-13582)  New platform-agnostic extensions for arrays: `contentEquals` to compare arrays' 
    content for equality, `contentHashCode` to get hashcode of array's content, and `contentToString` to get the string representation of array elements.
- [`KT-14510`](https://youtrack.jetbrains.com/issue/KT-14510) Generic constraints of `Array.flatten` signature were relaxed a bit to make it just usable.
- [`KT-14789`](https://youtrack.jetbrains.com/issue/KT-14789) Provide `KotlinVersion` class, which allows to get the current version of the standard 
    library and compare it with some other `KotlinVersion` value.

### IDE
- [`KT-14409`](https://youtrack.jetbrains.com/issue/KT-14409) Incorrect "Variable can be declared immutable" inspection for local delegated variable
- [`KT-14431`](https://youtrack.jetbrains.com/issue/KT-14431) Create quick-fix on UNUSED_PARAMETER/VARIABLE when it can be replaced with one underscore
- [`KT-14794`](https://youtrack.jetbrains.com/issue/KT-14794) Add /Specify type/Remove explicit type intentions for property with getters if type 
    can be inferred
- [`KT-14752`](https://youtrack.jetbrains.com/issue/KT-14752) Exception while typing @JsName annotation in editor

## 1.1-M02 (EAP-2)

### Language features

+ **Destructuring for lambdas** ([proposal](https://github.com/Kotlin/KEEP/issues/32))

    Current limitations:

    - Nested destructuring is not supported
    - Destructuring in named functions/constructors is not supported
    - Is not supported for JS target
        
### Compiler

#### Smart cast enhancements
- [`KT-2127`](https://youtrack.jetbrains.com/issue/KT-2127) Smart cast receiver to not null after a not null safe call
- [`KT-6840`](https://youtrack.jetbrains.com/issue/KT-6840) Make data flow information the same for assigned and assignee
- [`KT-13426`](https://youtrack.jetbrains.com/issue/KT-13426) Fix exception when smartcast on both dispatch & extension receiver

#### Bound references related issues
- [`KT-12995`](https://youtrack.jetbrains.com/issue/KT-12995) Do not skip generation of the left-hand side for intrinsic bound references and class literals
- [`KT-13075`](https://youtrack.jetbrains.com/issue/KT-13075) Fix codegen for bound class reference
- [`KT-13110`](https://youtrack.jetbrains.com/issue/KT-13110) Fix type mismatch error on class literal with integer receiver expression
- [`KT-13172`](https://youtrack.jetbrains.com/issue/KT-13172) Report error on "this::class" in super constructor call
- [`KT-13271`](https://youtrack.jetbrains.com/issue/KT-13271) Fix incorrect unsupported error on synthetic extension call on LHS of ::
- [`KT-13367`](https://youtrack.jetbrains.com/issue/KT-13367) Inline bound callable reference if it's used only as a lambda

#### Coroutines related issues
- [`KT-13156`](https://youtrack.jetbrains.com/issue/KT-13156) Do not execute last Unit-typed coroutine statement twice
- [`KT-13246`](https://youtrack.jetbrains.com/issue/KT-13246) Fix VerifyError with coroutines on Dalvik
- [`KT-13289`](https://youtrack.jetbrains.com/issue/KT-13289) Fix VerifyError with coroutines: Bad type on operand stack
- [`KT-13409`](https://youtrack.jetbrains.com/issue/KT-13409) Fix generic variable spilling with coroutines
- [`KT-13531`](https://youtrack.jetbrains.com/issue/KT-13531) Fix ClassCastException when coercion to Unit interacts with generic await() and coroutines
- Prohibit `Continuation<*>` as a last parameter of suspend functions
- [`KT-13560`](https://youtrack.jetbrains.com/issue/KT-13560) Prohibit non-Unit suspend functions

#### Typealises related issues
- [`KT-13200`](https://youtrack.jetbrains.com/issue/KT-13200) Fix incorrect number of required type arguments reported on typealias
- [`KT-13181`](https://youtrack.jetbrains.com/issue/KT-13181) Fix unresolved reference for a type alias from a different module
- [`KT-13161`](https://youtrack.jetbrains.com/issue/KT-13161) Support java static methods calls with typealiases
- [`KT-13835`](https://youtrack.jetbrains.com/issue/KT-13835) Do not lose nullability information  while expanding type alias in projection position
- [`KT-13422`](https://youtrack.jetbrains.com/issue/KT-13422) Prohibit usage of type alias to exception class as an object in 'throw' expression 
- [`KT-13735`](https://youtrack.jetbrains.com/issue/KT-13735) Fix NoSuchMethodError for generic typealias access
- [`KT-13513`](https://youtrack.jetbrains.com/issue/KT-13513) Support SAM constructors for aliased java functional types
- [`KT-13822`](https://youtrack.jetbrains.com/issue/KT-13822) Fix exception for start-projection of a type alias
- [`KT-14071`](https://youtrack.jetbrains.com/issue/KT-14071) Prohibit using type alias as a qualifier for super
- [`KT-14282`](https://youtrack.jetbrains.com/issue/KT-14282) Report error on unused type alias with -language-version 1.0
- [`KT-14274`](https://youtrack.jetbrains.com/issue/KT-14274) Fix type alias resolution when it's used for supertype constructor call

#### JDK dependent built-in classes related issues
- [`KT-13209`](https://youtrack.jetbrains.com/issue/KT-13209) Change first parameter's type of Map.getOrDefault to K instead of Any
- [`KT-13069`](https://youtrack.jetbrains.com/issue/KT-13069) Do not emit invalid DefaultImpls delegation when interface extends MutableMap with JDK8

#### `data` classes and inheritance
- [`KT-11306`](https://youtrack.jetbrains.com/issue/KT-11306) Allow data classes to implement equals/hashCode/toString from base classes

#### Various JVM code generation issues
- [`KT-13182`](https://youtrack.jetbrains.com/issue/KT-13182) Fix compiler internal error at inline
- [`KT-13757`](https://youtrack.jetbrains.com/issue/KT-13757) Prohibit referencing nested classes by name with $
- [`KT-12985`](https://youtrack.jetbrains.com/issue/KT-12985) Do not create range instances for 'for' loop in CharSequence.indices
- [`KT-13931`](https://youtrack.jetbrains.com/issue/KT-13931) Optimize generated code for IntRange#contains

#### Various analysis & diagnostic issues
- [`KT-435`](https://youtrack.jetbrains.com/issue/KT-435) Use parameter names in error messages when calling a function-valued expression
- [`KT-10001`](https://youtrack.jetbrains.com/issue/KT-10001) Fix false unnecessary non-null assertion on a pair element
- [`KT-12811`](https://youtrack.jetbrains.com/issue/KT-12811) Treat function declaration as final if it is a member of a final class
- [`KT-13961`](https://youtrack.jetbrains.com/issue/KT-13961) Report REDECLARATION on private-in-file 'foo' vs public 'foo' in different file

### JS

#### Feature support
- [`KT-13544`](https://youtrack.jetbrains.com/issue/KT-13544) Support type aliases in JS
- [`KT-13345`](https://youtrack.jetbrains.com/issue/KT-13345) Support class literals in JS

#### Library updates
- [`KT-18`](https://youtrack.jetbrains.com/issue/KT-18) Move exceptions from `java.lang` to `kotlin` package
- [`KT-12386`](https://youtrack.jetbrains.com/issue/KT-12386) Rewrite JS collections in Kotlin, move them to `kotlin.collections` package
- [`KT-7809`](https://youtrack.jetbrains.com/issue/KT-7809) Make Collection implementations conform to their declared interfaces
- [`KT-7473`](https://youtrack.jetbrains.com/issue/KT-7473) Make AbstractCollection.equals check object type
- [`KT-13429`](https://youtrack.jetbrains.com/issue/KT-13429) Make 'remove' on fresh iterator throw exception  instead of removing last element
- [`KT-13459`](https://youtrack.jetbrains.com/issue/KT-13459) Make JS implementation of ArrayList::add(index, element) check the index is in valid range
- [`KT-8724`](https://youtrack.jetbrains.com/issue/KT-8724) Fix MutableIterator.remove() for HashMap
- [`KT-10786`](https://youtrack.jetbrains.com/issue/KT-10786) Make Map.keys return view of map keys instead of snapshot
- [`KT-14194`](https://youtrack.jetbrains.com/issue/KT-14194) Make HashMap.putAll implementation not to call getKey/getValue

### Standard Library

#### Backward compatibility
- [`KT-14297`](https://youtrack.jetbrains.com/issue/KT-14297) Add @SinceKotlin annotation to support compatibility with compilation against older standard library
- [`KT-14213`](https://youtrack.jetbrains.com/issue/KT-14213) Ensure printStackTrace can be called with -language-version 1.0

#### Enhancements
- [`KEEP-53`](https://github.com/Kotlin/KEEP/blob/master/proposals/stdlib/abstract-collections.md) Provide two distinct hierarchies of abstract collections: one for implementing read-only/immutable collections, and other for implementing mutable collections
- [`KEEP-13`](https://github.com/Kotlin/KEEP/blob/master/proposals/stdlib/map-copying.md) Provide extension functions to copy maps
- [`KT-18`](https://youtrack.jetbrains.com/issue/KT-18) Introduce type aliases for common exceptions from `java.lang` in `kotlin` package
- [`KT-12762`](https://youtrack.jetbrains.com/issue/KT-12762) Make `kotlin.ranges.until` return an empty range for "illegal" 'to' parameter
- [`KT-12894`](https://youtrack.jetbrains.com/issue/KT-12894) Allow nullable receiver for `use` extension

### Reflection

#### New features
- [`KT-8998`](https://youtrack.jetbrains.com/issue/KT-8998) Introduce comprehensive API to work with KType instances 
- [`KT-10447`](https://youtrack.jetbrains.com/issue/KT-10447) Provide a way to check if a KClass is a data class
- [`KT-11284`](https://youtrack.jetbrains.com/issue/KT-11284) Add KClass<T>.cast extension
- [`KT-13106`](https://youtrack.jetbrains.com/issue/KT-13106) Support annotation constructors in reflection

#### Optimizations
- [`KT-10651`](https://youtrack.jetbrains.com/issue/KT-10651) Optimize KClass.simpleName

### IDE

###### New features
- [`KT-12903`](https://youtrack.jetbrains.com/issue/KT-12903) Implement "Inline type alias" refactoring
- [`KT-12902`](https://youtrack.jetbrains.com/issue/KT-12902) Implement "Introduce type alias" refactoring
- [`KT-12904`](https://youtrack.jetbrains.com/issue/KT-12904) Implement "Create type alias from usage" quick fix
- [`KT-9016`](https://youtrack.jetbrains.com/issue/KT-9016) Make use of named higher order function parameters
- [`KT-12205`](https://youtrack.jetbrains.com/issue/KT-12205) Suggest import of Kotlin static members in editor with Java source
- [`KT-13941`](https://youtrack.jetbrains.com/issue/KT-13941) Implement intention for introducing destructured lambda parameters when it's possible
- [`KT-13943`](https://youtrack.jetbrains.com/issue/KT-13943) Implement inspection and quickfix for to detect a manual destructuring of for / lambda parameter

###### Issues fixed
- [`KT-13004`](https://youtrack.jetbrains.com/issue/KT-13004) Support bound method references in completion
- [`KT-13242`](https://youtrack.jetbrains.com/issue/KT-13242) Suggest 'typealias' keyword in completion
- [`KT-13244`](https://youtrack.jetbrains.com/issue/KT-13244) Override/Implement Members: Do not expand type aliases in the generated members
- [`KT-13611`](https://youtrack.jetbrains.com/issue/KT-13611) Go to Class: Fix presentation of type aliases
- [`KT-13759`](https://youtrack.jetbrains.com/issue/KT-13759) Rename: Process object-wrapping alias references
- [`KT-13955`](https://youtrack.jetbrains.com/issue/KT-13955) Find Usages: Add special type for usages inside of type aliases
- [`KT-13479`](https://youtrack.jetbrains.com/issue/KT-13479) Support navigation to type aliases from binaries
- [`KT-13766`](https://youtrack.jetbrains.com/issue/KT-13766) Fix optimize imports not to add wrong and unnecessary import because of type alias
- [`KT-12949`](https://youtrack.jetbrains.com/issue/KT-12949) Consider type aliases as candidates for import
- [`KT-13266`](https://youtrack.jetbrains.com/issue/KT-13266) Suggest non-imported type aliases in completion
- [`KT-13689`](https://youtrack.jetbrains.com/issue/KT-13689) Do not treat type alias constructor usage as original type usage for optimize imports

### Scripting

- A new library `kotlin-script-util` containing utilities for implementing kotlin script support  
- [`KT-7880`](https://youtrack.jetbrains.com/issue/KT-7880) Experimental support for JSR 223 Scripting API
- [`KT-13975`](https://youtrack.jetbrains.com/issue/KT-13975), [`KT-14264`](https://youtrack.jetbrains.com/issue/KT-14264) Convert error on retrieving gradle plugin settings to warning
- Implement support for custom template-based scripts in command-line compiler, maven and gradle plugins

## 1.1-M01 (EAP-1)

### Language features

+ **Coroutines (async/await, generators)** ([proposal](https://github.com/Kotlin/kotlin-coroutines))

    Current limitations:

    - for some cases type inference is not supported yet
    - limited IDE support
    - allowed only one `handleResult` function: [design](https://github.com/Kotlin/kotlin-coroutines/blob/master/kotlin-coroutines-informal.md#result-handlers)
    - handling `finally` blocks is not supported: [issue](https://github.com/Kotlin/kotlin-coroutines/issues/1)

+ **Bound callable references** ([proposal](https://github.com/Kotlin/KEEP/issues/5))

+ **Type aliases** ([proposal](https://github.com/Kotlin/KEEP/issues/4))

    Current limitations:
    - type alias constructors for inner classes are not supported yet
    - annotations on type alias are not supported yet
    - limited IDE support

+ **Local delegated properties** ([proposal](https://github.com/Kotlin/KEEP/issues/25))

+ **JDK dependent built-in classes** ([proposal](https://github.com/Kotlin/KEEP/issues/30))

+ **Sealed class inheritors in the same file** ([proposal](https://github.com/Kotlin/KEEP/issues/29))
+ **Allow base classes for data classes** ([proposal](https://github.com/Kotlin/KEEP/issues/31))


### Scripting

- Implement support for [Script Definition Template](https://github.com/Kotlin/KEEP/blob/da9f3ec5f78429e7560bfc284cb7f52e02282b1f/proposals/script-definition-template.md)
and related functionality, except the following parts:
  - automatic script templates discovery is not implemented
  - `@file:ScriptTemplate` annotation is not supported
  - the parameters `javaHome` and `scripts` from `KotlinScriptExternalDependencies` are not used yet
- Implement support for custom template-based scripts in IDEA: resolving, completion and navigation to symbols from script classpath and sources
- Implement GradleScriptTemplatesProvider extension that supplies a script template if gradle with
[kotlin script support](https://github.com/gradle/gradle-script-kotlin) is used in the project


### Compiler

###### Issues fixed
- [`KT-4779`](https://youtrack.jetbrains.com/issue/KT-4779) Generate default methods for implementations in interfaces
- [`KT-11780`](https://youtrack.jetbrains.com/issue/KT-11780) Fixed incorrect "No cast needed" warning
- [`KT-12156`](https://youtrack.jetbrains.com/issue/KT-12156) Fixed incorrect error on `inline` modifier inside final class
- [`KT-12358`](https://youtrack.jetbrains.com/issue/KT-12358) Report missing error "Abstract member not implemented" when a fake method of 'Any' is inherited from an interface
- [`KT-6206`](https://youtrack.jetbrains.com/issue/KT-6206) Generate equals/hashCode/toString in data class always unless it'll cause a JVM signature clash error
- [`KT-8990`](https://youtrack.jetbrains.com/issue/KT-8990) Fixed incorrect error "virtual member hidden" for a private method of an inner class
- [`KT-12429`](https://youtrack.jetbrains.com/issue/KT-12429) Fixed visibility checks for annotation usage on top-level declarations
- [`KT-5068`](https://youtrack.jetbrains.com/issue/KT-5068) Introduced a special diagnostic message for "type mismatch" errors such as `fun f(): Int = { 1 }`.

### Standard Library

- [`KT-8254`](https://youtrack.jetbrains.com/issue/KT-8254) Provide standard library supplement artifacts for using with JDK 7 and 8.
These artifacts include extensions for the types available in the latter JDKs, such as `AutoCloseable.use` ([`KT-5899`](https://youtrack.jetbrains.com/issue/KT-5899)) or `Stream.toList`.
- [`KT-12753`](https://youtrack.jetbrains.com/issue/KT-12753) Provide an access to named group matches of `Regex` match result (for JDK 8 only).
- Add `assertFails` overload with message to kotlin-test.


### IDE

###### New features

+ [`KT-12019`](https://youtrack.jetbrains.com/issue/KT-12019) Introduce "redundant `if`" inspection

###### Issues fixed

+ [`KT-12389`](https://youtrack.jetbrains.com/issue/KT-12389) Do not exit from REPL when toString() of user class throws an exception
+ [`KT-12129`](https://youtrack.jetbrains.com/issue/KT-12129) Fixed link on api reference page in KDoc

## 1.0.7

### IDE

- Project View: Fix presentation of Kotlin files and their members when @JvmName having the same name as the file itself
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
- [`KT-15444`](https://youtrack.jetbrains.com/issue/KT-15444) Spring Support: Consider declaration open if it's supplemented with a preconfigured annotation in corresponding compiler plugin

#### Intention actions, inspections and quickfixes

##### New features

- [`KT-15068`](https://youtrack.jetbrains.com/issue/KT-15068) Implement intention which rename file according to the top-level class name
- Implement quickfix which enables/disables coroutine support in module or project
- [`KT-15056`](https://youtrack.jetbrains.com/issue/KT-15056) Implement intention which converts object literal to class
- [`KT-8855`](https://youtrack.jetbrains.com/issue/KT-8855) Implement "Create label" quick fix
- [`KT-15627`](https://youtrack.jetbrains.com/issue/KT-15627) Support "Change parameter type" for parameters with type-mismatched default value

## 1.0.6

### IDE

- [`KT-13811`](https://youtrack.jetbrains.com/issue/KT-13811) Expose JVM target setting in IntelliJ IDEA plugin compiler configuration UI
- [`KT-12410`](https://youtrack.jetbrains.com/issue/KT-12410) Expose language version setting in IntelliJ IDEA plugin compiler configuration UI

#### Intention actions, inspections and quickfixes

- [`KT-14569`](https://youtrack.jetbrains.com/issue/KT-14569) Convert Property to Function Intention: Search occurrences using progress dialog
- [`KT-14501`](https://youtrack.jetbrains.com/issue/KT-14501) Create from Usage: Support array access expressions/binary expressions with type mismatch errors
- [`KT-14500`](https://youtrack.jetbrains.com/issue/KT-14500) Create from Usage: Suggest functional type based on the call with lambda argument and unresolved invoke()
- [`KT-14459`](https://youtrack.jetbrains.com/issue/KT-14459) Initialize with Constructor Parameter: Fix IDE freeze on properties in generic class
- [`KT-14044`](https://youtrack.jetbrains.com/issue/KT-14044) Fix exception on deleting unused declaration in IDEA 2016.3
- [`KT-14019`](https://youtrack.jetbrains.com/issue/KT-14019) Create from Usage: Support generation of abstract members for superclasses
- [`KT-14246`](https://youtrack.jetbrains.com/issue/KT-14246) Intentions: Convert function type parameter to receiver
- [`KT-14246`](https://youtrack.jetbrains.com/issue/KT-14246) Intentions: Convert function type receiver to parameter

##### New features

- [`KT-14729`](https://youtrack.jetbrains.com/issue/KT-14729) Implement "Add names to call arguments" intention
- [`KT-11760`](https://youtrack.jetbrains.com/issue/KT-11760) Create from Usage: Support adding type parameters to the referenced type

#### Refactorings

- [`KT-14583`](https://youtrack.jetbrains.com/issue/KT-14583) Change Signature: Use new signature when looking for redeclaration conflicts
- [`KT-14854`](https://youtrack.jetbrains.com/issue/KT-14854) Extract Interface: Fix NPE on dialog opening
- [`KT-14814`](https://youtrack.jetbrains.com/issue/KT-14814) Rename: Fix renaming of .kts file to .kt and vice versa
- [`KT-14361`](https://youtrack.jetbrains.com/issue/KT-14361) Rename: Do not report redeclaration conflict for private top-level declarations located in different files
- [`KT-14596`](https://youtrack.jetbrains.com/issue/KT-14596) Safe Delete: Fix exception on deleting Java class used in Kotlin import directive(s)
- [`KT-14325`](https://youtrack.jetbrains.com/issue/KT-14325) Rename: Fix exceptions on moving file with facade class to another package
- [`KT-14197`](https://youtrack.jetbrains.com/issue/KT-14197) Move: Fix callable reference processing when moving to another package
- [`KT-13781`](https://youtrack.jetbrains.com/issue/KT-13781) Extract Function: Do not wrap companion member references inside of the `with` call

##### New features
- [`KT-14792`](https://youtrack.jetbrains.com/issue/KT-14792) Rename: Suggest respective parameter name for the local variable passed to function

## 1.0.5

### IDE

- [`KT-9125`](https://youtrack.jetbrains.com/issue/KT-9125) Support Type Hierarchy on references inside of super type call entries
- [`KT-13542`](https://youtrack.jetbrains.com/issue/KT-13542) Rename: Do not search parameter text occurrences outside of its containing declaration
- [`KT-8672`](https://youtrack.jetbrains.com/issue/KT-8672) Rename: Optimize search of parameter references in calls with named arguments
- [`KT-9285`](https://youtrack.jetbrains.com/issue/KT-9285) Rename: Optimize search of private class members
- [`KT-13589`](https://youtrack.jetbrains.com/issue/KT-13589) Use TODO() consistently in implementation stubs
- [`KT-13630`](https://youtrack.jetbrains.com/issue/KT-13630) Do not show Change Signature dialog when applying "Remove parameter" quick-fix
- Re-highlight only single function after local modifications
- [`KT-13474`](https://youtrack.jetbrains.com/issue/KT-13474) Fix performance of typing super call lambda
- Show "Variables and values captured in a closure" highlighting only for usages
- [`KT-13838`](https://youtrack.jetbrains.com/issue/KT-13838) Add file name to the presentation of private top-level declaration (Go to symbol, etc.)
- [`KT-14096`](https://youtrack.jetbrains.com/issue/KT-14096) Rename: When renaming Kotlin file outside of source root do not rename its namesake in a source root
- [`KT-13928`](https://youtrack.jetbrains.com/issue/KT-13928) Move Inner Class to Upper Level: Fix replacement of outer class instances used in inner class constructor calls
- [`KT-12556`](https://youtrack.jetbrains.com/issue/KT-12556) Allow using whitespaces and other symbols in "Generate -> Test Function" dialog
- [`KT-14122`](https://youtrack.jetbrains.com/issue/KT-14122) Generate 'toString()': Permit for data classes
- [`KT-12398`](https://youtrack.jetbrains.com/issue/KT-12398) Call Hierarchy: Show Kotlin usages of Java methods
- [`KT-13976`](https://youtrack.jetbrains.com/issue/KT-13976) Search Everywhere: Render function parameter types
- [`KT-13977`](https://youtrack.jetbrains.com/issue/KT-13977) Search Everywhere: Render extension type in prefix position
- Implement Kotlin facet

#### Intention actions, inspections and quickfixes

- [`KT-9490`](https://youtrack.jetbrains.com/issue/KT-9490) Convert receiver to parameter: use template instead of the dialog
- [`KT-11483`](https://youtrack.jetbrains.com/issue/KT-11483) Move to Companion: Do not use qualified names as labels
- [`KT-13874`](https://youtrack.jetbrains.com/issue/KT-13874) Move to Companion: Fix AssertionError on running refactoring from Conflicts View
- [`KT-13883`](https://youtrack.jetbrains.com/issue/KT-13883) Move to Companion Object: Fix exception when applied to class
- [`KT-13876`](https://youtrack.jetbrains.com/issue/KT-13876) Move to Companion Object: Forbid for functions/properties referencing type parameters of the containing class
- [`KT-13877`](https://youtrack.jetbrains.com/issue/KT-13877) Move to Companion Object: Warn if companion object already contains function with the same signature
- [`KT-13933`](https://youtrack.jetbrains.com/issue/KT-13933) Convert Parameter to Receiver: Do not qualify companion members with labeled 'this'
- [`KT-13942`](https://youtrack.jetbrains.com/issue/KT-13942) Redundant 'toString()' in String Template: Disable for qualified expressions with 'super' receiver
- [`KT-13878`](https://youtrack.jetbrains.com/issue/KT-13878) Remove Redundant Receiver Parameter: Fix exception receiver removal
- [`KT-14143`](https://youtrack.jetbrains.com/issue/KT-14143) Create from Usages: Do not suggest on type-mismatched expressions which are not call arguments
- [`KT-13882`](https://youtrack.jetbrains.com/issue/KT-13882) Convert Receiver to Parameter: Fix AssertionError
- [`KT-14199`](https://youtrack.jetbrains.com/issue/KT-14199) Add Library: Fix exception due to resolution being run in the "dumb mode"
- Convert Receiver to Parameter: Fix this replacement

##### New features

- [`KT-11525`](https://youtrack.jetbrains.com/issue/KT-11525) Implement "Create type parameter" quickfix
- [`KT-9931`](https://youtrack.jetbrains.com/issue/KT-9931) Implement "Remove unused assignment" quickfix
- [`KT-14245`](https://youtrack.jetbrains.com/issue/KT-14245) Implement "Convert enum to sealed class" intention
- [`KT-14245`](https://youtrack.jetbrains.com/issue/KT-14245) Implement "Convert sealed class to enum" intention

#### Refactorings

- [`KT-13535`](https://youtrack.jetbrains.com/issue/KT-13535) Pull Up: Remove visibility modifiers on adding 'override'
- [`KT-13216`](https://youtrack.jetbrains.com/issue/KT-13216) Move: Report separate conflicts for each property accessor
- [`KT-13216`](https://youtrack.jetbrains.com/issue/KT-13216) Move: Forbid moving of enum entries
- [`KT-13553`](https://youtrack.jetbrains.com/issue/KT-13553) Move: Do not show directory selection dialog if target directory is already specified by drag-and-drop
- [`KT-8867`](https://youtrack.jetbrains.com/issue/KT-8867) Rename: Rename all overridden members if user chooses to refactor base declaration(s)
- Pull Up: Drop 'override' modifier if moved member doesn't override anything
- [`KT-13660`](https://youtrack.jetbrains.com/issue/KT-13660) Move: Do not drop object receivers when calling variable of extension functional type
- [`KT-13903`](https://youtrack.jetbrains.com/issue/KT-13903) Move: Remove companion object which becomes empty after the move
- [`KT-13916`](https://youtrack.jetbrains.com/issue/KT-13916) Move: Report visibility conflicts in import directives
- [`KT-13906`](https://youtrack.jetbrains.com/issue/KT-13906) Move Nested Class to Upper Level: Do not show directory selection dialog twice
- [`KT-13901`](https://youtrack.jetbrains.com/issue/KT-13901) Move: Do not ignore target directory selected in the dialog (DnD mode)
- [`KT-13904`](https://youtrack.jetbrains.com/issue/KT-13904) Move Nested Class to Upper Level: Preserve state of "Search in comments"/"Search for text occurrences" checkboxes
- [`KT-13909`](https://youtrack.jetbrains.com/issue/KT-13909) Move Files/Directories: Fix behavior of "Open moved files in editor" checkbox
- [`KT-14004`](https://youtrack.jetbrains.com/issue/KT-14004) Introduce Variable: Fix exception on trying to extract variable of functional type
- [`KT-13726`](https://youtrack.jetbrains.com/issue/KT-13726) Move: Fix bogus conflicts due to references resolving to wrong library version
- [`KT-14114`](https://youtrack.jetbrains.com/issue/KT-14114) Move: Fix exception on moving Kotlin file without declarations
- [`KT-14157`](https://youtrack.jetbrains.com/issue/KT-14157) Rename: Rename do-while loop variables in the loop condition
- [`KT-14128`](https://youtrack.jetbrains.com/issue/KT-14128), [`KT-13862`](https://youtrack.jetbrains.com/issue/KT-13862) Rename: Use qualified class name when looking for occurrences in non-code files
- [`KT-6199`](https://youtrack.jetbrains.com/issue/KT-6199) Rename: Replace non-code class occurrences with new qualified name
- [`KT-14182`](https://youtrack.jetbrains.com/issue/KT-14182) Move: Show error message on applying to enum entries
- Extract Function: Support implicit abnormal exits via Nothing-typed expressions
- [`KT-14285`](https://youtrack.jetbrains.com/issue/KT-14285) Rename: Forbid on backing field reference
- [`KT-14240`](https://youtrack.jetbrains.com/issue/KT-14240) Introduce Variable: Do not replace assignment left-hand sides
- [`KT-14234`](https://youtrack.jetbrains.com/issue/KT-14234) Rename: Do not suggest type-based names for functions with primitive return types

##### New features

- [`KT-13155`](https://youtrack.jetbrains.com/issue/KT-13155) Implement "Introduce Type Parameter" refactoring
- [`KT-11017`](https://youtrack.jetbrains.com/issue/KT-11017) Implement "Extract Superclass" refactoring
- [`KT-11017`](https://youtrack.jetbrains.com/issue/KT-11017) Implement "Extract Interface" refactoring
Pull Up: Support properties declared in the primary constructor
Pull Up: Support members declared in the companion object of the original class
Pull Up: Show member dependencies in the refactoring dialog
- [`KT-9485`](https://youtrack.jetbrains.com/issue/KT-9485) Push Down: Support moving members from Java to Kotlin class
- [`KT-13963`](https://youtrack.jetbrains.com/issue/KT-13963) Rename: Implement popup chooser for overriding members

#### Android Lint

###### Issues fixed

- [`KT-12022`](https://youtrack.jetbrains.com/issue/KT-12022) Report lint warnings even when file contains errors

## 1.0.4

### Compiler

#### Analysis & diagnostics

- [`KT-10968`](https://youtrack.jetbrains.com/issue/KT-10968), [`KT-11075`](https://youtrack.jetbrains.com/issue/KT-11075), [`KT-12286`](https://youtrack.jetbrains.com/issue/KT-12286) Type inference of callable references
- [`KT-11892`](https://youtrack.jetbrains.com/issue/KT-11892) Report error on qualified super call to a supertype extended by a different supertype
- [`KT-12875`](https://youtrack.jetbrains.com/issue/KT-12875) Report error on incorrect call of member extension invoke
- [`KT-12847`](https://youtrack.jetbrains.com/issue/KT-12847) Report error on accessing protected property setter from super class' companion
- [`KT-12322`](https://youtrack.jetbrains.com/issue/KT-12322) Overload resolution ambiguity with constructor reference when class has a companion object
- [`KT-11440`](https://youtrack.jetbrains.com/issue/KT-11440) Overload resolution ambiguity on specialized Map.put implementation from Java
- [`KT-11389`](https://youtrack.jetbrains.com/issue/KT-11389) Runtime exception when calling Java primitive overloadings
- [`KT-8200`](https://youtrack.jetbrains.com/issue/KT-8200) Exception when using non-generic interface with generic arguments
- [`KT-10237`](https://youtrack.jetbrains.com/issue/KT-10237) Exception on an unresolved symbol in a type parameter bound in the 'where' clause
- [`KT-11821`](https://youtrack.jetbrains.com/issue/KT-11821) Exception on incorrect number of generic arguments in a type parameter bound in the 'where' clause
- [`KT-12482`](https://youtrack.jetbrains.com/issue/KT-12482) Exception: Implementation doesn't have the most specific type, but none of the other overridden methods does either
- [`KT-12687`](https://youtrack.jetbrains.com/issue/KT-12687) Exception when 'data' modifier is applied to object
- [`KT-9620`](https://youtrack.jetbrains.com/issue/KT-9620) AssertionError in DescriptorResolver#checkBounds
- [`KT-3689`](https://youtrack.jetbrains.com/issue/KT-3689) IllegalAccess on a property with private setter of the subclass
- [`KT-6391`](https://youtrack.jetbrains.com/issue/KT-6391) Wrong warning for array casting (Array<Any?> to Array<Any>)
- [`KT-8596`](https://youtrack.jetbrains.com/issue/KT-8596) Exception when analyzing nested class constructor reference in an argument position
- [`KT-12982`](https://youtrack.jetbrains.com/issue/KT-12982) Incorrect type inference when accessing mutable protected property via reflection
- [`KT-13206`](https://youtrack.jetbrains.com/issue/KT-13206) Report "Cast never succeeds" if and only if ClassCastException can be predicted
- [`KT-12467`](https://youtrack.jetbrains.com/issue/KT-12467) IllegalStateException: Concrete fake override should have exactly one concrete super-declaration: []
- [`KT-13340`](https://youtrack.jetbrains.com/issue/KT-13340) Report "return is not allowed here" only on the return keyword, not the whole expression
- [`KT-2349`](https://youtrack.jetbrains.com/issue/KT-2349), [`KT-6054`](https://youtrack.jetbrains.com/issue/KT-6054) Report "uninitialized enum entry" if enum entry is referenced before its declaration
- [`KT-12809`](https://youtrack.jetbrains.com/issue/KT-12809) Report "uninitialized variable" if property is referenced before its declaration
- [`KT-260`](https://youtrack.jetbrains.com/issue/KT-260) Do not report "cast never succeeds" when casting nullable to nullable
- [`KT-11769`](https://youtrack.jetbrains.com/issue/KT-11769) Prohibit access from enum instance initialization code to members of enum's companion object
- [`KT-13371`](https://youtrack.jetbrains.com/issue/KT-13371) Fix CompilationException: Rewrite at slice LEAKING_THIS key: REFERENCE_EXPRESSION
- [`KT-13401`](https://youtrack.jetbrains.com/issue/KT-13401) Fix StackOverflowError when checking variance
- [`KT-13330`](https://youtrack.jetbrains.com/issue/KT-13330), [`KT-13349`](https://youtrack.jetbrains.com/issue/KT-13349) Fix AssertionError: Illegal resolved call to variable with invoke
- [`KT-13421`](https://youtrack.jetbrains.com/issue/KT-13421) Fix AssertionError: Only integer constants should be checked for overflow
- [`KT-13555`](https://youtrack.jetbrains.com/issue/KT-13555) Fix internal error "resolveToInstruction"
- [`KT-8989`](https://youtrack.jetbrains.com/issue/KT-8989) Change error messages: Replace "invisible_fake" with "invisible (private in a supertype)"
- [`KT-13612`](https://youtrack.jetbrains.com/issue/KT-13612) Val reassignment in try / catch
- [`KT-5469`](https://youtrack.jetbrains.com/issue/KT-5469) Incorrect "is never used" warning for value used in catch block
- [`KT-13510`](https://youtrack.jetbrains.com/issue/KT-13510) Missing "Nested class not allowed" error for anonymous object inside val initializer
- [`KT-13685`](https://youtrack.jetbrains.com/issue/KT-13685) Fix NPE when resolving callable references on incomplete code
- Change error messages: Fix quotes around keywords in diagnostic messages
- Change error messages: Remove quotes around visibilities

#### Parser

- [`KT-7118`](https://youtrack.jetbrains.com/issue/KT-7118) Improve error message after trailing dot in floating point literal
- [`KT-4948`](https://youtrack.jetbrains.com/issue/KT-4948) Recover by following keyword
- [`KT-7915`](https://youtrack.jetbrains.com/issue/KT-7915) Recover after val with no subsequent name
- [`KT-12987`](https://youtrack.jetbrains.com/issue/KT-12987) Recover after val with no name before declaration starting with soft keyword

#### JVM code generation

- [`KT-12909`](https://youtrack.jetbrains.com/issue/KT-12909) Do not generate redundant bridge for special built-in override
- [`KT-11915`](https://youtrack.jetbrains.com/issue/KT-11915) Exception in entrySet when Map implementation in Kotlin extends another one
- [`KT-12755`](https://youtrack.jetbrains.com/issue/KT-12755) Exception on property generation in multi-file classes
- [`KT-12983`](https://youtrack.jetbrains.com/issue/KT-12983) VerifyError: Bad type on operand stack in arraylength
- [`KT-12908`](https://youtrack.jetbrains.com/issue/KT-12908) Variable initialization in loop causes VerifyError: Bad local variable type
- [`KT-13040`](https://youtrack.jetbrains.com/issue/KT-13040) Invalid bytecode generated for extension lambda invocation with safe call
- [`KT-13023`](https://youtrack.jetbrains.com/issue/KT-13023) Char operations throw ClassCastException for boxed Chars
- [`KT-11634`](https://youtrack.jetbrains.com/issue/KT-11634) Exception for super call in delegation
- [`KT-12359`](https://youtrack.jetbrains.com/issue/KT-12359) Redundant stubs are generated on inheriting from java.util.Collection
- [`KT-11833`](https://youtrack.jetbrains.com/issue/KT-11833) Error generating constructors of class on anonymous object inheriting from nested class of super class
- [`KT-13133`](https://youtrack.jetbrains.com/issue/KT-13133) Incorrect InnerClasses attribute value for anonymous object copied from an inline function
- [`KT-13241`](https://youtrack.jetbrains.com/issue/KT-13241) Indices optimization leads to VerifyError with smart cast receiver
- [`KT-13374`](https://youtrack.jetbrains.com/issue/KT-13374) Fix compiler exception when inline function contains anonymous object implementing an interface by delegation

##### Generated code performance

- [`KT-11964`](https://youtrack.jetbrains.com/issue/KT-11964) No TABLESWITCH in when on enum bytecode if enum constant is imported
- [`KT-6916`](https://youtrack.jetbrains.com/issue/KT-6916) Optimize 'for' over 'downTo'
- [`KT-12733`](https://youtrack.jetbrains.com/issue/KT-12733) Optimize 'for' over 'rangeTo' as a non-qualified call

### Standard Library

- [`KT-13115`](https://youtrack.jetbrains.com/issue/KT-13115), [`KT-13297`](https://youtrack.jetbrains.com/issue/KT-13297) Improve documentation formatting, clarify documentation for `FileTreeWalk`, `Sequence` and `generateSequence`.
- [`KT-12894`](https://youtrack.jetbrains.com/issue/KT-12894) Do not fail in `Closeable.use` if the resource is `null`.

### Reflection

- [`KT-12915`](https://youtrack.jetbrains.com/issue/KT-12915) Runtime exception on callBy of JvmStatic function with default arguments
- [`KT-12967`](https://youtrack.jetbrains.com/issue/KT-12967) Runtime exception on reference to generic property
- [`KT-13370`](https://youtrack.jetbrains.com/issue/KT-13370) NullPointerException on companionObjectInstance of a built-in class
- [`KT-13462`](https://youtrack.jetbrains.com/issue/KT-13462) Make KClass for primitive type equal to the corresponding KClass for wrapper type

### IDE

- [`KT-12655`](https://youtrack.jetbrains.com/issue/KT-12655) New Kotlin file: extra error message for already existing file
- [`KT-12760`](https://youtrack.jetbrains.com/issue/KT-12760) Prohibit running non-Unit returning main function
- [`KT-12893`](https://youtrack.jetbrains.com/issue/KT-12893) Impossible to open Kotlin compiler settings
- [`KT-10433`](https://youtrack.jetbrains.com/issue/KT-10433) Copy-pasting reference to companion object member causes import dialog
- [`KT-12803`](https://youtrack.jetbrains.com/issue/KT-12803) Class is marked as unused when it is only used is in method reference
- [`KT-13084`](https://youtrack.jetbrains.com/issue/KT-13084) Run test method action executes all tests from same kotlin file
- [`KT-12718`](https://youtrack.jetbrains.com/issue/KT-12718) Deadlock due to index reentering
- [`KT-13114`](https://youtrack.jetbrains.com/issue/KT-13114) 'Unused declaration' option 'JUnit static methods' is always enabled
- [`KT-12997`](https://youtrack.jetbrains.com/issue/KT-12997) Override/Implement Members: Support "Copy JavaDoc" options for library classes
- [`KT-12887`](https://youtrack.jetbrains.com/issue/KT-12887) "Extend selection" should select call's invoked expression
- [`KT-13383`](https://youtrack.jetbrains.com/issue/KT-13383), [`KT-13379`](https://youtrack.jetbrains.com/issue/KT-13379) Override/Implement Members: Do not make return type non-nullable if base return type is explicitly nullable
- [`KT-13218`](https://youtrack.jetbrains.com/issue/KT-13218) Extract Function: Fix AssertionError on callable references
- [`KT-6520`](https://youtrack.jetbrains.com/issue/KT-6520) Introduce 'maino' and 'psvmo' templates for generating main in object
- [`KT-13455`](https://youtrack.jetbrains.com/issue/KT-13455) Override/Implement: Make return type non-nullable (platform collection case) when overriding Java method
- [`KT-10209`](https://youtrack.jetbrains.com/issue/KT-10209) Find Usages: Do not duplicate containing declaration in super member warning dialog
- [`KT-12977`](https://youtrack.jetbrains.com/issue/KT-12977) Hybrid dependency causes "outdated binary" warning to appear in non-js project
- [`KT-13057`](https://youtrack.jetbrains.com/issue/KT-13057) Go to inheritors on Enum should navigate to all enum classes
- Fix exception when choose Gradle configurer after project is synced
- Allow configuring Kotlin in Gradle module without Kotlin sources
- Show all Kotlin annotations when browsing hierarchy of "java.lang.Annotation"

#### Completion

- [`KT-12793`](https://youtrack.jetbrains.com/issue/KT-12793) Suggest abstract protected extension methods

#### Performance

- [`KT-12645`](https://youtrack.jetbrains.com/issue/KT-12645) Lazily calculate FQ name for local classes
- [`KT-13071`](https://youtrack.jetbrains.com/issue/KT-13071) Fix severe freezes because of long lint checks on large files

#### Highlighting

- [`KT-12937`](https://youtrack.jetbrains.com/issue/KT-12937) Java synthetic accessors highlighting does not differ from local variables

#### KDoc

- [`KT-12998`](https://youtrack.jetbrains.com/issue/KT-12998) Backslash is not rendered
- [`KT-12999`](https://youtrack.jetbrains.com/issue/KT-12999) Backtick inside inline code block is not rendered
- [`KT-13000`](https://youtrack.jetbrains.com/issue/KT-13000) Exclamation mark is not rendered
- [`KT-10398`](https://youtrack.jetbrains.com/issue/KT-10398) Fully qualified link is not resolved in editor
- [`KT-12932`](https://youtrack.jetbrains.com/issue/KT-12932) Link to library element is not clickable
- [`KT-10654`](https://youtrack.jetbrains.com/issue/KT-10654) Quick Doc can't follow KDoc link in referenced function description
- [`KT-9271`](https://youtrack.jetbrains.com/issue/KT-9271) Show Quick Doc for implicit lambda parameter 'it'

#### Formatter

- [`KT-12830`](https://youtrack.jetbrains.com/issue/KT-12830) Remove spaces before *?* in nullable types
- [`KT-13314`](https://youtrack.jetbrains.com/issue/KT-13314) Format spaces around !is and !in

#### Intention actions, inspections and quickfixes

##### New features

- [`KT-12152`](https://youtrack.jetbrains.com/issue/KT-12152) "Leaking this" inspection reports dangerous operations inside constructors including:

   * Accessing non-final property in constructor
   * Calling non-final function in constructor
   * Using 'this' as function argument in constructor of non-final class

- [`KT-13187`](https://youtrack.jetbrains.com/issue/KT-13187) "Make constructor parameter a val" should make the val private or public depending on its option
- [`KT-5771`](https://youtrack.jetbrains.com/issue/KT-5771) Mark setter parameter type as redundant and provide quickfix to remove it
- [`KT-9228`](https://youtrack.jetbrains.com/issue/KT-9228) Add quickfix to remove '@' from annotation used as argument of another annotation
- [`KT-12251`](https://youtrack.jetbrains.com/issue/KT-12251) Add quickfix to fix type mismatch for primitive literals
- [`KT-12838`](https://youtrack.jetbrains.com/issue/KT-12838) Add quickfix for "Illegal usage of inline parameter" that adds `noinline`
- [`KT-13134`](https://youtrack.jetbrains.com/issue/KT-13134) Add quickfix for wrong Long suffix (Use `L` instead of `l`)
- [`KT-10903`](https://youtrack.jetbrains.com/issue/KT-10903) Add intention to convert lambda to function reference
- [`KT-7492`](https://youtrack.jetbrains.com/issue/KT-7492) Support "Create abstract function/property" inside an abstract class
- [`KT-10668`](https://youtrack.jetbrains.com/issue/KT-10668) Support "Create member/extension" corresponding to the extension receiver of enclosing function
- [`KT-12553`](https://youtrack.jetbrains.com/issue/KT-12553) Show versions in inspection about different version of Kotlin plugin in Maven and IDE plugin
- [`KT-12489`](https://youtrack.jetbrains.com/issue/KT-12489) Implement intention to replace camel-case test function name with a space-separated one
- [`KT-12730`](https://youtrack.jetbrains.com/issue/KT-12730) Warn about using different versions of Kotlin Gradle plugin and bundled compiler
- [`KT-13173`](https://youtrack.jetbrains.com/issue/KT-13173) Handle more cases in "Add Const Modifier" Intention
- [`KT-12628`](https://youtrack.jetbrains.com/issue/KT-12628) Quickfix for `invoke` operator unsafe calls
- [`KT-11425`](https://youtrack.jetbrains.com/issue/KT-11425) Inspection and quickfix to replace usages of `equals()` and `compareTo()` with operators
- [`KT-13113`](https://youtrack.jetbrains.com/issue/KT-13113) Inspection to detect redundant string templates
- [`KT-13011`](https://youtrack.jetbrains.com/issue/KT-13011) Inspection and quickfix for unnecessary lateinit
- [`KT-10731`](https://youtrack.jetbrains.com/issue/KT-10731) Inspection and quickfix for unnecessary use of toString() inside string interpolation
- [`KT-12043`](https://youtrack.jetbrains.com/issue/KT-12043) Intention to add / remove braces for when entry/entries
- [`KT-13483`](https://youtrack.jetbrains.com/issue/KT-13483) Intention to replace `a..b-1` with `a until b` and vice versa
- [`KT-6975`](https://youtrack.jetbrains.com/issue/KT-6975) Quickfix for adding 'inline' to a function with reified generic

##### Bugfixes

- Show receiver type in the text of "Create extension" quick fix
- Show target class name in the text of "Create member" quick fix
- [`KT-12869`](https://youtrack.jetbrains.com/issue/KT-12869) Usages of overridden Java method through synthetic accessors are not found
- [`KT-12813`](https://youtrack.jetbrains.com/issue/KT-12813) "Find Usages" for property returns function calls
- [`KT-7722`](https://youtrack.jetbrains.com/issue/KT-7722) Approximate unresolvable types in "Create from Usage" quickfixes
- [`KT-11115`](https://youtrack.jetbrains.com/issue/KT-11115) Implement Members: Fix base member detection when abstract and non-abstract members with matching signatures are inherited from an interface
- [`KT-12876`](https://youtrack.jetbrains.com/issue/KT-12876) Bogus suggestion to move property to constructor
- [`KT-13055`](https://youtrack.jetbrains.com/issue/KT-13055) Exception in "Specify Type Explicitly" intention
- [`KT-12942`](https://youtrack.jetbrains.com/issue/KT-12942) "Replace 'when' with 'if'" intention changes semantics when 'if' statements are used
- [`KT-12646`](https://youtrack.jetbrains.com/issue/KT-12646) 'Convert to block body' should use partial body resolve
- [`KT-12919`](https://youtrack.jetbrains.com/issue/KT-12919) Use simple class name in "Change function return type" quickfix
- [`KT-13151`](https://youtrack.jetbrains.com/issue/KT-13151) Incorrect warning "Make variable immutable"
- [`KT-13170`](https://youtrack.jetbrains.com/issue/KT-13170) "Declaration has platform type" inspection: by default should not be reported for platform type arguments
- [`KT-13262`](https://youtrack.jetbrains.com/issue/KT-13262) "Wrap with safe let call" quickfix produces wrong result for qualified function
- [`KT-13364`](https://youtrack.jetbrains.com/issue/KT-13364) Do not suggest creating annotations/enum classes for unresolved type parameter bounds
- [`KT-12627`](https://youtrack.jetbrains.com/issue/KT-12627) Allow warnings suppression for secondary constructor
- [`KT-13365`](https://youtrack.jetbrains.com/issue/KT-13365) Disable "Create property" (non-abstract) in interfaces. Make "Create function" (non-abstract) generate function body in interfaces
- [`KT-8903`](https://youtrack.jetbrains.com/issue/KT-8903) Remove Unused Receiver: update function/property usages
- [`KT-11799`](https://youtrack.jetbrains.com/issue/KT-11799) Create from Usage: Make extension functions/properties 'private' by default
- [`KT-11795`](https://youtrack.jetbrains.com/issue/KT-11795) Create from Usage: Place extension properties after the usage and generate stub getter
- [`KT-12951`](https://youtrack.jetbrains.com/issue/KT-12951) Prohibit "Convert to expression body" when function body is 'if' without 'else' or 'when' is non-exhaustive
- [`KT-13430`](https://youtrack.jetbrains.com/issue/KT-13430) "Add non-null asserted (!!) call" quickfix can't process unary operators
- [`KT-13336`](https://youtrack.jetbrains.com/issue/KT-13336) "Convert concatenation to template" intention appends literal to variable omitting braces
- [`KT-13328`](https://youtrack.jetbrains.com/issue/KT-13328) Do not suggest "Replace infix with safe call" inside conditions or binary / unary expressions
- [`KT-13452`](https://youtrack.jetbrains.com/issue/KT-13452) "Replace if expression with assignment" doesn't work for cascade if-else if-else
- [`KT-13184`](https://youtrack.jetbrains.com/issue/KT-13184) "Different Kotlin Version" inspection: false positive caused by verbose plugin version name
- [`KT-13480`](https://youtrack.jetbrains.com/issue/KT-13480) "Can be replaced with comparison" inspection: false positive if extension method called 'equals' is used
- [`KT-13288`](https://youtrack.jetbrains.com/issue/KT-13288) "Unused property" inspection: false positive when extending abstract class and implementing interface
- [`KT-13432`](https://youtrack.jetbrains.com/issue/KT-13432) "Replace with safe call" quickfix does not work with `compareTo()` usage
- [`KT-13444`](https://youtrack.jetbrains.com/issue/KT-13444) "Invert if" intention changes semantics for nested if with return
- [`KT-13536`](https://youtrack.jetbrains.com/issue/KT-13536) Fix StackOverflowError from "Unused Symbol" inspection after importing enum's values()
- [`KT-12820`](https://youtrack.jetbrains.com/issue/KT-12820) Platform Type Inspection: !! quickfix shouldn't be available when any generic parameter has platform type
- [`KT-9825`](https://youtrack.jetbrains.com/issue/KT-9825) Incorrect "unused variable" warning when used in finally block
- [`KT-13715`](https://youtrack.jetbrains.com/issue/KT-13715) Prohibit applying "Change to star projection" to functional types

#### Refactorings

##### New features

- [`KT-12017`](https://youtrack.jetbrains.com/issue/KT-12017) Inline Property: Support "Do not show this dialog" and "Inline this occurrence" options

##### Bugfixes

- [`KT-11176`](https://youtrack.jetbrains.com/issue/KT-11176) Add a space before '{' in functions generated "Generate hashCode/equals/toString"
- [`KT-12294`](https://youtrack.jetbrains.com/issue/KT-12294) Introduce Property: Fix extraction of expressions referring to primary constructor parameters
- [`KT-12413`](https://youtrack.jetbrains.com/issue/KT-12413) Change Signature: Fix bogus warning about unresolved type parameters/invalid functional type replacement
- [`KT-12084`](https://youtrack.jetbrains.com/issue/KT-12084) Introduce Property: Do not skip outer classes if extractable expression is contained in object literal
- [`KT-13082`](https://youtrack.jetbrains.com/issue/KT-13082) Rename: Fix exception on property rename preview
- [`KT-13207`](https://youtrack.jetbrains.com/issue/KT-13207) Safe delete: Fix exception when removing any function in 2016.2
- [`KT-12945`](https://youtrack.jetbrains.com/issue/KT-12945) Rename: Fix function description in super method warning dialog
- [`KT-12922`](https://youtrack.jetbrains.com/issue/KT-12922) Introduce Variable: Do not suggest expressions without type
- [`KT-12943`](https://youtrack.jetbrains.com/issue/KT-12943) Rename: Show function signatures in "Rename Overloads" dialog
- [`KT-13157`](https://youtrack.jetbrains.com/issue/KT-13157) Extract Function: Automatically quote function name if necessary
- [`KT-13010`](https://youtrack.jetbrains.com/issue/KT-13010) Extract Function: Fix generation of destructuring declarations
- [`KT-13128`](https://youtrack.jetbrains.com/issue/KT-13128) Introduce Variable: Retain entered name after changing "Specify type explicitly" option
- [`KT-13054`](https://youtrack.jetbrains.com/issue/KT-13054) Introduce Variable: Skip leading/trailing comments inside selection
- [`KT-13385`](https://youtrack.jetbrains.com/issue/KT-13385) Move: Quote package name (if necessary) when moving declarations to new file
- [`KT-13395`](https://youtrack.jetbrains.com/issue/KT-13395) Introduce Property: Fix duplicate count in popup window
- [`KT-13277`](https://youtrack.jetbrains.com/issue/KT-13277) Change Signature: Fix usage processing to prevent interfering with Python support plugin
- [`KT-13254`](https://youtrack.jetbrains.com/issue/KT-13254) Rename: Conflict detection for type parameters
- [`KT-13282`](https://youtrack.jetbrains.com/issue/KT-13282), [`KT-13283`](https://youtrack.jetbrains.com/issue/KT-13283) Rename: Fix name quoting for automatic renamers
- [`KT-13239`](https://youtrack.jetbrains.com/issue/KT-13239) Rename: Warn about function name conflicts
- [`KT-13174`](https://youtrack.jetbrains.com/issue/KT-13174) Move: Warn about accessibility conflicts due to moving to unrelated module
- [`KT-13175`](https://youtrack.jetbrains.com/issue/KT-13175) Move: Warn about accessibility conflicts when moving entire file
- [`KT-13240`](https://youtrack.jetbrains.com/issue/KT-13240) Rename: Do not report shadowing conflict if redeclaration is detected
- [`KT-13253`](https://youtrack.jetbrains.com/issue/KT-13253) Rename: Report conflicts for constructor parameters
- [`KT-12971`](https://youtrack.jetbrains.com/issue/KT-12971) Push Down: Do not specifiy visibility on generated overriding members
- [`KT-13124`](https://youtrack.jetbrains.com/issue/KT-13124) Pull Up: Skip super members without explicit declarations
- [`KT-13032`](https://youtrack.jetbrains.com/issue/KT-13032) Rename: Support accessors with non-conventional names
- [`KT-13463`](https://youtrack.jetbrains.com/issue/KT-13463) Rename: Quote parameter name when necessary
- [`KT-13476`](https://youtrack.jetbrains.com/issue/KT-13476) Rename: Fix parameter rename when new name matches call selector
- [`KT-9381`](https://youtrack.jetbrains.com/issue/KT-9381) Rename: Do not search for component convention usages
- [`KT-13488`](https://youtrack.jetbrains.com/issue/KT-13488) Rename: Support rename of packages with non-standard quoted names

#### Debugger

##### New features

- [`KT-7549`](https://youtrack.jetbrains.com/issue/KT-7549) Provide an option to use the Kotlin syntax when evaluating watches and expressions in Java files

##### Bugfixes

- [`KT-13059`](https://youtrack.jetbrains.com/issue/KT-13059) Fix error stepping on *Step Over* action in the end of while block
- [`KT-13037`](https://youtrack.jetbrains.com/issue/KT-13037) Fix possible deadlock in debugger in 2016.1 and exception in 2016.2
- [`KT-12651`](https://youtrack.jetbrains.com/issue/KT-12651) Fix exception in evaluate expression when bad identifier is used for marking object
- [`KT-12896`](https://youtrack.jetbrains.com/issue/KT-12896) Fix "Step In" to inline functions for Android
- [`KT-13269`](https://youtrack.jetbrains.com/issue/KT-13269) Make quick evaluate work on receiver in qualified expressions
- [`KT-12641`](https://youtrack.jetbrains.com/issue/KT-12641) Unknown error on evaluate expression containing inline functions with complicated environment
- [`KT-13163`](https://youtrack.jetbrains.com/issue/KT-13163) Fix exception when evaluating expression: Access is allowed from event dispatch thread only.

### JS

#### New features

- [`KT-3008`](https://youtrack.jetbrains.com/issue/KT-3008) Option to generate require.js and AMD compatible modules
- [`KT-5987`](https://youtrack.jetbrains.com/issue/KT-5987) Add ability to refer to class
- [`KT-4115`](https://youtrack.jetbrains.com/issue/KT-4115) Provide method to get Kotlin type name

#### Bugfixes

- [`KT-8003`](https://youtrack.jetbrains.com/issue/KT-8003) Compiler exception on 'throw throw'
- [`KT-8318`](https://youtrack.jetbrains.com/issue/KT-8318) Wrong result for 'when' containing only 'else' block
- [`KT-12157`](https://youtrack.jetbrains.com/issue/KT-12157) Compiler exception on `when` condition containing `return`, `break` or `continue`
- [`KT-12275`](https://youtrack.jetbrains.com/issue/KT-12275) Fix code generation with inline function call in condition of `while`/`do..while`
- [`KT-13160`](https://youtrack.jetbrains.com/issue/KT-13160) Fix compiler exception when left-hand side of assignment is array access and right-hand side is inline function
- [`KT-12864`](https://youtrack.jetbrains.com/issue/KT-12864) Make enums comparable
- [`KT-12865`](https://youtrack.jetbrains.com/issue/KT-12865) Implementing Comparable breaks inheritance
- [`KT-12928`](https://youtrack.jetbrains.com/issue/KT-12928) Nested inline causes undefined reference access
- [`KT-12929`](https://youtrack.jetbrains.com/issue/KT-12929) Code with callable reference crashed at runtime (in some JS VMs)
- [`KT-13043`](https://youtrack.jetbrains.com/issue/KT-13043) Invalid invocation generated for secondary constructor that calls constructor from base class with default parameters
- [`KT-13025`](https://youtrack.jetbrains.com/issue/KT-13025) 'function?.invoke' does not work properly with extension functions
- [`KT-12807`](https://youtrack.jetbrains.com/issue/KT-12807) Lambda was lost in generated code
- [`KT-12808`](https://youtrack.jetbrains.com/issue/KT-12808) Compiler duplicates arguments and the body of lambda when lambda is in RHS of assignment operator
- [`KT-12873`](https://youtrack.jetbrains.com/issue/KT-12873) Fix ReferenceError when class delegates to complex expression
- [`KT-13658`](https://youtrack.jetbrains.com/issue/KT-13658) Wrong code when capturing object


### Tools

#### Gradle

- Gradle versions < 2.0 are not supported
- [`KT-13234`](https://youtrack.jetbrains.com/issue/KT-13234) Setting kotlinOptions.destination and kotlinOptions.classpath is deprecated
- [`KT-9392`](https://youtrack.jetbrains.com/issue/KT-9392) Kotlin classes are missing after converting Java class to Kotlin
- [`KT-12736`](https://youtrack.jetbrains.com/issue/KT-12736) Kotlin classes are deleted when generated Java source is changed
- [`KT-12658`](https://youtrack.jetbrains.com/issue/KT-12658) Build fails after android resources are edited
- [`KT-12750`](https://youtrack.jetbrains.com/issue/KT-12750) Non clean compilation fails with gradle 2.14
- [`KT-12912`](https://youtrack.jetbrains.com/issue/KT-12912) New class from subproject is unresolved with subsequent build with Gradle Daemon
- [`KT-12962`](https://youtrack.jetbrains.com/issue/KT-12962) Incremental compilation: Track changes in generated files
- [`KT-12923`](https://youtrack.jetbrains.com/issue/KT-12923) Incremental compilation: Compile error when code using internal class is modified
- [`KT-13528`](https://youtrack.jetbrains.com/issue/KT-13528) Incremental compilation: support multi-project incremental compilation
- [`KT-13732`](https://youtrack.jetbrains.com/issue/KT-13732) Android Build folder littered with `copyFlavourTypeXXX`

#### KAPT

##### New features

- [`KT-13499`](https://youtrack.jetbrains.com/issue/KT-13499) Implement Annotation Processing API (JSR 269) natively in Kotlin

##### Bugfixes

- [`KT-12776`](https://youtrack.jetbrains.com/issue/KT-12776) Android build fails with KAPT and generateStubs depending on library module names
- [`KT-13179`](https://youtrack.jetbrains.com/issue/KT-13179) Java is recompiled every time with Gradle 2.14 and KAPT
- [`KT-12303`](https://youtrack.jetbrains.com/issue/KT-12303), [`KT-12113`](https://youtrack.jetbrains.com/issue/KT-12113) Do not pass non-relevant annotations to processors

#### REPL

- [`KT-12389`](https://youtrack.jetbrains.com/issue/KT-12389) REPL just quits when toString() of user class throws an exception

#### CLI & Ant

- [`KT-13237`](https://youtrack.jetbrains.com/issue/KT-13237) Include kotlin-reflect.jar to classpath by default, add '-no-reflect' key to suppress this behavior

#### CLI

- [`KT-13491`](https://youtrack.jetbrains.com/issue/KT-13491) Support '-no-reflect' in 'kotlin' command

#### Maven

- [`KT-13211`](https://youtrack.jetbrains.com/issue/KT-13211) Provide better compilation failure info for TeamCity builds

#### Compiler daemon

- Fix exception "java.lang.NoClassDefFoundError: Could not initialize class kotlin.Unit"

## 1.0.3

### Compiler

#### Analysis & diagnostics

- Combination of `open` and `override` is no longer a warning
- [`KT-4829`](https://youtrack.jetbrains.com/issue/KT-4829) Equal conditions in `when` is now a warning
- [`KT-6611`](https://youtrack.jetbrains.com/issue/KT-6611) "This cast can never succeed" warning is no longer reported for `Foo<T> as Foo<Any>`
- [`KT-7174`](https://youtrack.jetbrains.com/issue/KT-7174) Declaring members with the same signature as non-overridable methods from Java classes (like Object.wait/notify) is now an error (when targeting JVM)
- [`KT-12302`](https://youtrack.jetbrains.com/issue/KT-12302) `abstract` modifier for a member of interface is no longer a warning
- [`KT-12452`](https://youtrack.jetbrains.com/issue/KT-12452) `open` modifier for a member of interface without implementation is now a warning
- [`KT-11111`](https://youtrack.jetbrains.com/issue/KT-11111) Overriding by inline function is now a warning, overriding by a function with reified type parameter is an error
- [`KT-12337`](https://youtrack.jetbrains.com/issue/KT-12337) Reference to a property with invisible setter now has KProperty type (as opposed to KMutableProperty)

###### Issues fixed

- [`KT-4285`](https://youtrack.jetbrains.com/issue/KT-4285) No warning for a non-tail call when the method inherits default arguments from superclass
- [`KT-4764`](https://youtrack.jetbrains.com/issue/KT-4764) Spurious "Variable must be initialized" in try/catch/finally
- [`KT-6665`](https://youtrack.jetbrains.com/issue/KT-6665) Unresolved reference leads to marking subsequent code unreachable
- [`KT-11750`](https://youtrack.jetbrains.com/issue/KT-11750) Exceptions when creating various entries with the name "name" in enums
- [`KT-11998`](https://youtrack.jetbrains.com/issue/KT-11998) Smart cast to not-null is not performed on a boolean property in `if` condition
- [`KT-10648`](https://youtrack.jetbrains.com/issue/KT-10648) Exhaustiveness check does not work when sealed class hierarchy contains intermediate sealed classes
- [`KT-10717`](https://youtrack.jetbrains.com/issue/KT-10717) Type inference for lambda with local return
- [`KT-11266`](https://youtrack.jetbrains.com/issue/KT-11266) Fixed "Empty intersection of types" internal compiler error for some cases
- [`KT-11857`](https://youtrack.jetbrains.com/issue/KT-11857) Fix visibility check for dynamic members within protected method (when targeting JS)
- [`KT-12589`](https://youtrack.jetbrains.com/issue/KT-12589) Improved "`infix` modifier is inapplicable" diagnostic message
- [`KT-11679`](https://youtrack.jetbrains.com/issue/KT-11679) Erroneous call with argument causes Throwable at ResolvedCallImpl.getArgumentMapping()
- [`KT-12623`](https://youtrack.jetbrains.com/issue/KT-12623) Fix ISE on malformed code

#### JVM code generation

- [`KT-5075`](https://youtrack.jetbrains.com/issue/KT-5075) Optimize array/collection indices usage in `for` loop
- [`KT-11116`](https://youtrack.jetbrains.com/issue/KT-11116) Optimize coercion to Unit, POP operations are backward-propagated

###### Issues fixed
- [`KT-11499`](https://youtrack.jetbrains.com/issue/KT-11499) Compiler crashes with "Incompatible stack heights"
- [`KT-11943`](https://youtrack.jetbrains.com/issue/KT-11943) CompilationException with extension property of KClass
- [`KT-12125`](https://youtrack.jetbrains.com/issue/KT-12125) Wrong increment/decrement on Byte/Char/Short.MAX_VALUE/MIN_VALUE
- [`KT-12192`](https://youtrack.jetbrains.com/issue/KT-12192) Exhaustiveness check isn't generated for when expression returning Unit
- [`KT-12200`](https://youtrack.jetbrains.com/issue/KT-12200) Erroneously optimized away assignment to a property initialized to zero
- [`KT-12582`](https://youtrack.jetbrains.com/issue/KT-12582) "VerifyError: Bad local variable type" caused by explicit loop variable type
- [`KT-12708`](https://youtrack.jetbrains.com/issue/KT-12708) Bridge method not generated when data class implements interface with copy() method
- [`KT-12106`](https://youtrack.jetbrains.com/issue/KT-12106) import static of reified companion object method throws IllegalAccessError

#### Performance

- Reduced number of IO operation when loading kotlin compiled classes

#### Сompiler options

- Allow to specify version of Kotlin language for source compatibility with older releases.
    - CLI: `-language-version` command line option
    - Maven: `languageVersion` configuration parameter, linked with `kotlin.compiler.languageVersion` property
    - Gradle: `kotlinOptions.languageVersion` property in task configuration
- Allow to specify which java runtime target version to generate bytecode for.
    - CLI: `-jvm-target` command line option
    - Maven: `jvmTarget` configuration parameter, linked with `kotlin.compiler.jvmTarget` property
    - Gradle: `kotlinOptions.jvmTarget` property in task configuration
- Allow to specify path to JDK to resolve classes from.
    - CLI: `-jdk-home` command line option
    - Maven: `jdkHome` configuration parameter, linked with `kotlin.compiler.jdkHome` property
    - Gradle: `kotlinOptions.jdkHome` property in task configuration

### Standard Library

- Improve documentation (including [`KT-11632`](https://youtrack.jetbrains.com/issue/KT-11632))
- List iteration used in collection operations is performed with an indexed loop when the list supports `RandomAccess` and the operation isn't inlined

### IDE

#### Completion

###### New features

- Smart completion after `by` and `in`
- Improved completion in bodies of overridden members (when no type is specified)
- Improved presentation of completion items for property accessors
- Fixed keyword completion after `try` in assignment expression
- [`KT-8527`](https://youtrack.jetbrains.com/issue/KT-8527) Include non-imported declarations on the first completion
- [`KT-12068`](https://youtrack.jetbrains.com/issue/KT-12068) Special completion item for "[]" get-operator access
- [`KT-12080`](https://youtrack.jetbrains.com/issue/KT-12080) Parameter names are now higher up in completion list

###### Issues fixed
- Fixed enum members being present in completion as static members
- Fixed QuickDoc not working for properties generated for java classes
- [`KT-9166`](https://youtrack.jetbrains.com/issue/KT-9166) Code completion does not work for synthetic java properties on typing "g"
- [`KT-11609`](https://youtrack.jetbrains.com/issue/KT-11609) No named arguments completion should be after dot
- [`KT-11633`](https://youtrack.jetbrains.com/issue/KT-11633) Wrong indentation after completing a statement in data class
- [`KT-11680`](https://youtrack.jetbrains.com/issue/KT-11680) Code completion of label for existing return with value inserts redundant whitespace
- [`KT-11784`](https://youtrack.jetbrains.com/issue/KT-11784) Completion for `if` statement should add parentheses automatically
- [`KT-11890`](https://youtrack.jetbrains.com/issue/KT-11890) Completion for callable references does not propose static Java members
- [`KT-11912`](https://youtrack.jetbrains.com/issue/KT-11912) String interpolation is not converted to ${} form when accessing this.property
- [`KT-11957`](https://youtrack.jetbrains.com/issue/KT-11957) No `catch` and `finally` keywords in completion
- [`KT-12103`](https://youtrack.jetbrains.com/issue/KT-12103) Smart completion for nested SAM-adapter produces short unresolved name
- [`KT-12138`](https://youtrack.jetbrains.com/issue/KT-12138) Do not show "::error" in smart completion when any function type accepting one argument is expected
- [`KT-12150`](https://youtrack.jetbrains.com/issue/KT-12150) Smart completion suggests to compare non-nullable with null
- [`KT-12124`](https://youtrack.jetbrains.com/issue/KT-12124) No code completion for a java property in a specific position
- [`KT-12299`](https://youtrack.jetbrains.com/issue/KT-12299) Completion: incorrect priority of property foo over method getFoo in Kotlin-only code
- [`KT-12328`](https://youtrack.jetbrains.com/issue/KT-12328) Qualified function name inserted when typing before `if`
- [`KT-12427`](https://youtrack.jetbrains.com/issue/KT-12427) Completion doesn't work for "@receiver:" annotation target
- [`KT-12447`](https://youtrack.jetbrains.com/issue/KT-12447) Don't use CompletionProgressIndicator in Kotlin plugin
- [`KT-12669`](https://youtrack.jetbrains.com/issue/KT-12669) Completion should show variant with `()` when there is default lambda
- [`KT-12369`](https://youtrack.jetbrains.com/issue/KT-12369) Pressing dot after class name should not cause insertion of constructor call

#### Spring support

###### New features

- [`KT-11692`](https://youtrack.jetbrains.com/issue/KT-11692) Support Spring model diagrams for Kotlin classes
- [`KT-12079`](https://youtrack.jetbrains.com/issue/KT-12079) Support "Autowired members defined in invalid Spring bean" inspection on Kotlin declarations
- [`KT-12092`](https://youtrack.jetbrains.com/issue/KT-12092) Implement bean references in @Qualifier annotations
- [`KT-12135`](https://youtrack.jetbrains.com/issue/KT-12135) Automatically configure components based on `basePackageClasses` attribute of @ComponentScan
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
- [`KT-8477`](https://youtrack.jetbrains.com/issue/KT-8477) New inspection "Can be primary constructor property" with quick-fix
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
- [`KT-11864`](https://youtrack.jetbrains.com/issue/KT-11864) Suggest "Create function/secondary constructor" quick fix on argument type mismatch
- [`KT-11876`](https://youtrack.jetbrains.com/issue/KT-11876) Quickfix for "Extension function type is not allowed as supertype" error
- [`KT-11920`](https://youtrack.jetbrains.com/issue/KT-11920) "Increase visibility" and "Decrease visibility" quickfixes for exposed visibility errors
- [`KT-12089`](https://youtrack.jetbrains.com/issue/KT-12089) Quickfix "Make primary constructor parameter a property"
- [`KT-12121`](https://youtrack.jetbrains.com/issue/KT-12121) "Add `toString()` call" quickfix
- [`KT-11104`](https://youtrack.jetbrains.com/issue/KT-11104) New quickfixes for nullability problems: "Surround with null check" and "Wrap with safe let call"
- [`KT-12310`](https://youtrack.jetbrains.com/issue/KT-12310) New inspection "Member has platform type" with quickfix

###### Issues fixed

- Fixed "Convert property initializer getter" intention being available inside lambda initializer
- Improved message for "Can be declared as `val`" inspection
- [`KT-3797`](https://youtrack.jetbrains.com/issue/KT-3797) Quickfix to make a function abstract should not be offered for object members
- [`KT-11866`](https://youtrack.jetbrains.com/issue/KT-11866) Suggest "Create secondary constructor" when constructors exist but are not applicable
- [`KT-11482`](https://youtrack.jetbrains.com/issue/KT-11482) Fixed exception in "Move to companion object" intention
- [`KT-11483`](https://youtrack.jetbrains.com/issue/KT-11483) Pass implicit receiver as argument when moving member function to companion object
- [`KT-11512`](https://youtrack.jetbrains.com/issue/KT-11512) Allow choosing any source root in "Move file to directory" intention
- [`KT-10950`](https://youtrack.jetbrains.com/issue/KT-10950) Keep original file package name when moving top-level declarations to separate file (provided it's not ambiguous)
- [`KT-10174`](https://youtrack.jetbrains.com/issue/KT-10174) Optimize imports after applying "Move declaration to separate file" intention
- [`KT-11764`](https://youtrack.jetbrains.com/issue/KT-11764) Intention "Replace with a `forEach` function call should replace `continue` with `return@forEach`
- [`KT-11724`](https://youtrack.jetbrains.com/issue/KT-11724) False suggestion to replace with compound assignment
- [`KT-11805`](https://youtrack.jetbrains.com/issue/KT-11805) Invert if-condition intention breaks code in case of end of line comment
- [`KT-11811`](https://youtrack.jetbrains.com/issue/KT-11811) "Make protected" intention for a val declared in parameters of constructor
- [`KT-11710`](https://youtrack.jetbrains.com/issue/KT-11710) "Replace `if` with elvis operator": incorrect code generated for `if` expression
- [`KT-11849`](https://youtrack.jetbrains.com/issue/KT-11849) Replace explicit parameter with `it` changes the meaning of code because of the shadowing
- [`KT-11870`](https://youtrack.jetbrains.com/issue/KT-11870) "Replace with Elvis" refactoring doesn't change the variable type from T? to T
- [`KT-12069`](https://youtrack.jetbrains.com/issue/KT-12069) Specify language for all Kotlin code inspections
- [`KT-11366`](https://youtrack.jetbrains.com/issue/KT-11366) "object `Companion` is never used" warning in intellij
- [`KT-11275`](https://youtrack.jetbrains.com/issue/KT-11275) Inconsistent behaviour of "move lambda argument out of parentheses" intention action when using lambda calls with function arguments without parentheses
- [`KT-11594`](https://youtrack.jetbrains.com/issue/KT-11594) "Add non-null asserted (!!) call" applied to unsafe cast to nullable type causes AE at KtPsiFactory.createExpression()
- [`KT-11982`](https://youtrack.jetbrains.com/issue/KT-11982) False "Redundant final modifier" reported
- [`KT-12040`](https://youtrack.jetbrains.com/issue/KT-12040) "Replace when with if" produce invalid code for first entry which has comment
- [`KT-12204`](https://youtrack.jetbrains.com/issue/KT-12204) "Use classpath of module" option in existing Kotlin run configuration may be changed when a new run configuration is created
- [`KT-10635`](https://youtrack.jetbrains.com/issue/KT-10635) Don't mark private writeObject and readObject methods of Serializable classes as unused
- [`KT-11466`](https://youtrack.jetbrains.com/issue/KT-11466) "Make abstract" quick fix applies to outer class of object with accidentally abstract function
- [`KT-11120`](https://youtrack.jetbrains.com/issue/KT-11120) Constructor parameter/field reported as unused symbol even if it have `used` annotation
- [`KT-11974`](https://youtrack.jetbrains.com/issue/KT-11974) Invert if-condition intention loses comments
- [`KT-10812`](https://youtrack.jetbrains.com/issue/KT-10812) Globally unused constructors are not marked as such
- [`KT-11320`](https://youtrack.jetbrains.com/issue/KT-11320) Don't mark @BeforeClass (JUnit4) annotated functions as unused
- [`KT-12267`](https://youtrack.jetbrains.com/issue/KT-12267) "Change type" quick fix converts to Int for Long literal
- [`KT-11949`](https://youtrack.jetbrains.com/issue/KT-11949) Various problems fixed with "Constructor parameter is never used as a property" inspection
- [`KT-11716`](https://youtrack.jetbrains.com/issue/KT-11716) "Simply `for` using destructuring declaration" intention: incorrect behavior for data classes
- [`KT-12145`](https://youtrack.jetbrains.com/issue/KT-12145) "Simplify `for` using destructuring declaration" should work even when no variables declared inside loop
- [`KT-11933`](https://youtrack.jetbrains.com/issue/KT-11933) Entities used only by alias are marked as unused
- [`KT-12193`](https://youtrack.jetbrains.com/issue/KT-12193) Convert to block body isn't equivalent for when expressions returning Unit
- [`KT-10779`](https://youtrack.jetbrains.com/issue/KT-10779) Simplify `for` using destructing declaration: intention / inspection quick fix is available only when all variables are used
- [`KT-11281`](https://youtrack.jetbrains.com/issue/KT-11281) Fix exception on applying "Convert to class" intention to Java interface with Kotlin inheritor(s)
- [`KT-12285`](https://youtrack.jetbrains.com/issue/KT-12285) Fix exception on test class generation
- [`KT-12502`](https://youtrack.jetbrains.com/issue/KT-12502) Convert to expression body should be forbidden for non-exhaustive when returning Unit
- [`KT-12260`](https://youtrack.jetbrains.com/issue/KT-12260) ISE while replacing an operator with safe call
- [`KT-12649`](https://youtrack.jetbrains.com/issue/KT-12649) "Convert if to when" intention incorrectly deletes code
- [`KT-12671`](https://youtrack.jetbrains.com/issue/KT-12671) "Shot type" action: "Type is unknown" error on an invoked expression
- [`KT-12284`](https://youtrack.jetbrains.com/issue/KT-12284) Too wide applicability range for "Add braces to else" intention
- [`KT-11975`](https://youtrack.jetbrains.com/issue/KT-11975) "Invert if-condition" intention does not simplify `is` expression
- [`KT-12437`](https://youtrack.jetbrains.com/issue/KT-12437) "Replace explicit parameter" intention is suggested for parameter of inner lambda in presence of `it` from outer lambda
- [`KT-12290`](https://youtrack.jetbrains.com/issue/KT-12290) Navigate to the generated declaration when using "Implement abstract member" intention
- [`KT-12376`](https://youtrack.jetbrains.com/issue/KT-12376) Don't show "Package directive doesn't match file location" in injected code
- [`KT-12777`](https://youtrack.jetbrains.com/issue/KT-12777) Fix exception in "Create class" quickfix applied to unresolved references in type arguments

#### Language injection

- Apply injection for the literals in property initializer through property usages
- Enable injection from Java or Kotlin function declaration by annotating parameter with @Language annotation
- [`KT-2428`](https://youtrack.jetbrains.com/issue/KT-2428) Support basic use-cases of language injection for expressions marked with @Language annotation
- [`KT-11574`](https://youtrack.jetbrains.com/issue/KT-11574) Support predefined Java positions for language injection
- [`KT-11472`](https://youtrack.jetbrains.com/issue/KT-11472) Add comment or @Language annotation after "Inject language or reference" intention automatically

#### Refactorings

###### New features
- [`KT-6372`](https://youtrack.jetbrains.com/issue/KT-6372) Add name suggestions to Rename dialog
- [`KT-7851`](https://youtrack.jetbrains.com/issue/KT-7851) Respect naming conventions in automatic variable rename
- [`KT-8044`](https://youtrack.jetbrains.com/issue/KT-8044), [`KT-9432`](https://youtrack.jetbrains.com/issue/KT-9432) Support @JvmName annotation in rename refactoring
- [`KT-8512`](https://youtrack.jetbrains.com/issue/KT-8512) Support "Rename tests" options in Rename dialog
- [`KT-9168`](https://youtrack.jetbrains.com/issue/KT-9168) Support rename of synthetic properties
- [`KT-10578`](https://youtrack.jetbrains.com/issue/KT-10578) Support automatic test renaming for facade files
- [`KT-12657`](https://youtrack.jetbrains.com/issue/KT-12657) Rename implicit usages of annotation method `value`
- [`KT-12759`](https://youtrack.jetbrains.com/issue/KT-12759) Suggest renaming both property accessors with matching @JvmName when renaming one of them from Java

###### Issues fixed
- [`KT-4791`](https://youtrack.jetbrains.com/issue/KT-4791) Rename overridden property and all its accessors on attempt to rename overriding accessor in Java code
- [`KT-6363`](https://youtrack.jetbrains.com/issue/KT-6363) Do not rename ambiguous references in import directives
- [`KT-6663`](https://youtrack.jetbrains.com/issue/KT-6663) Fixed rename of ambiguous import reference to class/function when some referenced declarations are not changed
- [`KT-8510`](https://youtrack.jetbrains.com/issue/KT-8510) Preserve "Search in comments and strings" and "Search for text occurrences" settings in Rename dialog
- [`KT-8541`](https://youtrack.jetbrains.com/issue/KT-8541), [`KT-8786`](https://youtrack.jetbrains.com/issue/KT-8786) Do now show 'Rename overloads' options if target function has no overloads
- [`KT-8544`](https://youtrack.jetbrains.com/issue/KT-8544) Show more detailed description in Rename dialog
- [`KT-8562`](https://youtrack.jetbrains.com/issue/KT-8562) Show conflicts dialog on attempt of redeclaration
- [`KT-8611`](https://youtrack.jetbrains.com/issue/KT-8732) Qualify class references to resolve rename conflicts when possible
- [`KT-8732`](https://youtrack.jetbrains.com/issue/KT-8732) Implement Rename conflict analysis and fixes for properties/parameters
- [`KT-8860`](https://youtrack.jetbrains.com/issue/KT-8860) Allow renaming class by constructor delegation call referencing primary constructor
- [`KT-8892`](https://youtrack.jetbrains.com/issue/KT-8892) Suggest renaming base declarations on overriding members in object literals
- [`KT-9156`](https://youtrack.jetbrains.com/issue/KT-9156) Quote non-identifier names in Kotlin references
- [`KT-9157`](https://youtrack.jetbrains.com/issue/KT-9157) Fixed in-place rename of Kotlin expression referring Java declaration
- [`KT-9241`](https://youtrack.jetbrains.com/issue/KT-9241) Do not replace Java references to synthetic component functions when renaming constructor parameter
- [`KT-9435`](https://youtrack.jetbrains.com/issue/KT-9435) Process property accessor usages (Java) in comments and string literals
- [`KT-9444`](https://youtrack.jetbrains.com/issue/KT-9444) Rename dialog: Allow typing any identifier without backquotes
- [`KT-9446`](https://youtrack.jetbrains.com/issue/KT-9446) Copy default parameter values to overriding function which is renamed while its base function is not
- [`KT-9649`](https://youtrack.jetbrains.com/issue/KT-9649) Constraint search scope of parameter declared in a private member
- [`KT-10033`](https://youtrack.jetbrains.com/issue/KT-10033) Qualify references to members of enum companions in case of conflict with enum entries
- [`KT-10713`](https://youtrack.jetbrains.com/issue/KT-10713) Skip read-only declarations when renaming parameters
- [`KT-10687`](https://youtrack.jetbrains.com/issue/KT-10687) Qualify property references to avoid shadowing by parameters
- [`KT-11903`](https://youtrack.jetbrains.com/issue/KT-11903) Update references to facade class when renaming file via matching top-level class
- [`KT-12411`](https://youtrack.jetbrains.com/issue/KT-12411) Fix package name quotation in Move refactoring
- [`KT-12543`](https://youtrack.jetbrains.com/issue/KT-12543) Qualify property references with `this` to avoid renaming conflicts
- [`KT-12732`](https://youtrack.jetbrains.com/issue/KT-12732) Copy default parameter values to overriding function which is renamed by Java reference while its base function is unchanged
- [`KT-12747`](https://youtrack.jetbrains.com/issue/KT-12747) Fix exception on file copy

#### Java to Kotlin converter

###### New features

- [`KT-4727`](https://youtrack.jetbrains.com/issue/KT-4727) Convert Java code copied from browser or other sources

###### Issues fixed

- [`KT-11952`](https://youtrack.jetbrains.com/issue/KT-11952) Assertion failed in PropertyDetectionCache.get on conversion of access to Java constant of anonymous type
- [`KT-12046`](https://youtrack.jetbrains.com/issue/KT-12046) Recursive property setter
- [`KT-12039`](https://youtrack.jetbrains.com/issue/KT-12039) Static imports converted missing ".Companion"
- [`KT-12054`](https://youtrack.jetbrains.com/issue/KT-12054) Wrong conversion of `instanceof` checks with raw types
- [`KT-12045`](https://youtrack.jetbrains.com/issue/KT-12045) Convert `Object()` to `Any()`

#### Android Lint

###### Issues fixed

- [`KT-12015`](https://youtrack.jetbrains.com/issue/KT-12015) False positive for Bundle.getInt()
- [`KT-12023`](https://youtrack.jetbrains.com/issue/KT-12023) "minSdk" lint check doesn't work for `as`/`is`
- [`KT-12674`](https://youtrack.jetbrains.com/issue/KT-12674) "Calling new methods on older versions" errors for inlined constants
- [`KT-12681`](https://youtrack.jetbrains.com/issue/KT-12681) Running lint from main menu: diagnostics reported for java source files only
- [`KT-12173`](https://youtrack.jetbrains.com/issue/KT-12173) False positive for "Toast created but not shown" inside SAM adapter
- [`KT-12895`](https://youtrack.jetbrains.com/issue/KT-12895) NoSuchMethodError thrown when saving a Kotlin file

#### KDoc

###### New features
- Support for @receiver tag

###### Issues fixed
- Rendering of `_` and `*` standalone characters
- Rendering of code blocks
- [`KT-9933`](https://youtrack.jetbrains.com/issue/KT-9933) Indentation in code fragments is not preserved
- [`KT-10998`](https://youtrack.jetbrains.com/issue/KT-10998) Spaces around links are missing in return block
- [`KT-11791`](https://youtrack.jetbrains.com/issue/KT-11791) Markdown links rendering
- [`KT-12001`](https://youtrack.jetbrains.com/issue/KT-12001) Allow use of `@param` to document type parameter

#### Maven support

###### New features
- Inspections that check that kotlin IDEA plugin, kotlin Maven plugin and kotlin stdlib are of the same version
- [`KT-11643`](https://youtrack.jetbrains.com/issue/KT-11643) Inspections and intentions to fix erroneously configured Maven pom file
- [`KT-11701`](https://youtrack.jetbrains.com/issue/KT-11701) "Add Maven Dependency quick fix" in Kotlin source files
- [`KT-11743`](https://youtrack.jetbrains.com/issue/KT-11743) Intention to replace kotlin-test with kotlin-test-junit

###### Issues fixed
- [`KT-9492`](https://youtrack.jetbrains.com/issue/KT-9492) Configuring multiple Maven Modules
- [`KT-11642`](https://youtrack.jetbrains.com/issue/KT-11642) Kotlin Maven configurator tags order
- [`KT-11436`](https://youtrack.jetbrains.com/issue/KT-11436) "Choose Configurator" control opens dialogs with inconsistent modality (linux)
- [`KT-11731`](https://youtrack.jetbrains.com/issue/KT-11731) Default maven integration doesn't include documentation
- [`KT-12568`](https://youtrack.jetbrains.com/issue/KT-12568) Execution configuration: file path completion works only in some sub-elements of <sourceDirs>
- [`KT-12558`](https://youtrack.jetbrains.com/issue/KT-12558) Configure Kotlin in Project: "Undo" should revert changes in all poms
- [`KT-12512`](https://youtrack.jetbrains.com/issue/KT-12512) "Different IDE and Maven plugin version" inspection is being invoked for non-tracked pom.xml files

#### Debugger

###### New features
- [`KT-11438`](https://youtrack.jetbrains.com/issue/KT-11438) Support navigation from stacktrace to inline function call site

###### Issues fixed
- Do not step into inline lambda argument during step over inside inline function body
- Fix step over for inline argument with non-local return
- [`KT-12067`](https://youtrack.jetbrains.com/issue/KT-12067) Deadlock in Kotlin debugger is fixed
- [`KT-12232`](https://youtrack.jetbrains.com/issue/KT-12232) No code completion in Evaluate Expression and Throwable at CodeCompletionHandlerBase.invokeCompletion()
- [`KT-12137`](https://youtrack.jetbrains.com/issue/KT-12137) Evaluate expression: code completion/intention actions allows to use symbols from modules that are not referenced
- [`KT-12206`](https://youtrack.jetbrains.com/issue/KT-12206) NoSuchFieldError in Evaluate Expression on a property of a derived class
- [`KT-12678`](https://youtrack.jetbrains.com/issue/KT-12678) NoSuchFieldError in Evaluate Expression on accessing delegated property defined in other module
- [`KT-12773`](https://youtrack.jetbrains.com/issue/KT-12773) Fix debugging for Kotlin JS projects

#### Formatter

###### Issues fixed

- [`KT-12035`](https://youtrack.jetbrains.com/issue/KT-12035) Spaces around `as`
- [`KT-12018`](https://youtrack.jetbrains.com/issue/KT-12018) Spaces between function name and arguments in infix calls
- [`KT-11961`](https://youtrack.jetbrains.com/issue/KT-11961) Spaces before angle bracket in method definition
- [`KT-12175`](https://youtrack.jetbrains.com/issue/KT-12175) Don't enforce empty line between secondary constructors without body
- [`KT-12548`](https://youtrack.jetbrains.com/issue/KT-12548) Spaces around `is` keyword
- [`KT-12446`](https://youtrack.jetbrains.com/issue/KT-12446) Spaces before class type parameters
- [`KT-12634`](https://youtrack.jetbrains.com/issue/KT-12634) Spaces between method name and parenthesis in method call
- [`KT-10680`](https://youtrack.jetbrains.com/issue/KT-10680) Spaces around `in` keyword
- [`KT-12791`](https://youtrack.jetbrains.com/issue/KT-12791) Spaces between curly brace and expression inside string template
- [`KT-12781`](https://youtrack.jetbrains.com/issue/KT-12781) Spaces between annotation and expression
- [`KT-12689`](https://youtrack.jetbrains.com/issue/KT-12689) Spaces around semicolons
- [`KT-12714`](https://youtrack.jetbrains.com/issue/KT-12714) Spaces around parentheses in enum elements

#### Other

###### New features

- Added "Decompile" button to Kotlin bytecode toolwindow
- Added Kotlin "Tips of the day"
- Added "Kotlin 1.1 EAP" to "Configure Kotlin Plugin updates"
- [`KT-2919`](https://youtrack.jetbrains.com/issue/KT-2919) Constructor calls are no longer highlighted as classes
- [`KT-6540`](https://youtrack.jetbrains.com/issue/KT-6540) Infix function calls are now highlighted as regular function calls
- [`KT-9410`](https://youtrack.jetbrains.com/issue/KT-9410) Annotations in Kotlin are now highlighted with the same color as in Java by default
- [`KT-11465`](https://youtrack.jetbrains.com/issue/KT-11465) Type parameters in Kotlin are now highlighted with the same color as in Java by default
- [`KT-11657`](https://youtrack.jetbrains.com/issue/KT-11657) Allow viewing decompiled Java source code for Kotlin-compiled classes
- [`KT-11704`](https://youtrack.jetbrains.com/issue/KT-11704) Support file path references inside of Kotlin string literals
- [`KT-12076`](https://youtrack.jetbrains.com/issue/KT-12076) Kotlin Plugin update check: always display installed version number
- [`KT-11814`](https://youtrack.jetbrains.com/issue/KT-11814) New icon for kotlin annotation classes
- [`KT-12735`](https://youtrack.jetbrains.com/issue/KT-12735) Convert JavaDoc to KDoc when overriding Java class member in Kotlin

###### Issues fixed

- [`KT-5960`](https://youtrack.jetbrains.com/issue/KT-5960) Can't find usages for Java methods used from Kotlin by call convention
- [`KT-8362`](https://youtrack.jetbrains.com/issue/KT-8362) "New Kotlin file":  Keywords should be escaped in package name
- [`KT-8682`](https://youtrack.jetbrains.com/issue/KT-8682) Respect "Copy JavaDoc" option in the "Override/Implement Members..." dialog
- [`KT-8817`](https://youtrack.jetbrains.com/issue/KT-8817) Fixed rename of Java getters/setters through synthetic property references in Kotlin
- [`KT-9399`](https://youtrack.jetbrains.com/issue/KT-9399) Find Usages omits Kotlin annotation parameter usage in Java source
- [`KT-9797`](https://youtrack.jetbrains.com/issue/KT-9797) "Kotlin Bytecode" toolwindow breaks after closing
- [`KT-11145`](https://youtrack.jetbrains.com/issue/KT-11145) Use progress indicator when searching usages in Introduce Parameter
- [`KT-11155`](https://youtrack.jetbrains.com/issue/KT-11155) Allow running multiple Kotlin classes as well as running mixtures of Kotlin and Java classes
- [`KT-11495`](https://youtrack.jetbrains.com/issue/KT-11495) Show recursion line markers for extension function calls with different receiver
- [`KT-11659`](https://youtrack.jetbrains.com/issue/KT-11659) Generate abstract overrides for Any members inside of Kotlin interfaces
- [`KT-12070`](https://youtrack.jetbrains.com/issue/KT-12070) Add empty line in error message of Maven and Gradle configuration
- [`KT-11908`](https://youtrack.jetbrains.com/issue/KT-11908) Allow properties with custom setters to be used in generated equals/hashCode/toString
- [`KT-11617`](https://youtrack.jetbrains.com/issue/KT-11617) Fixed title of Introduce Parameter declaration chooser
- [`KT-11817`](https://youtrack.jetbrains.com/issue/KT-11817) Fixed rename of Kotlin enum constants through Java references
- [`KT-11816`](https://youtrack.jetbrains.com/issue/KT-11816) Fixed usages search for Safe Delete on simple enum entries
- [`KT-11282`](https://youtrack.jetbrains.com/issue/KT-11282) Delete interface reference from super-type list when applying Safe Delete to Java interface
- [`KT-11967`](https://youtrack.jetbrains.com/issue/KT-11967) Fix Find Usages/Rename for parameter references in XML files
- [`KT-10770`](https://youtrack.jetbrains.com/issue/KT-10770) "Optimize imports" will not keep import if a type is only referenced by kdoc
- [`KT-11955`](https://youtrack.jetbrains.com/issue/KT-11955) Copy/Paste inserts fully qualified name when copying function with overloads
- [`KT-12436`](https://youtrack.jetbrains.com/issue/KT-12436) "Replace explicit parameter with it": java.lang.Exception at BaseRefactoringProcessor.run()
- [`KT-12440`](https://youtrack.jetbrains.com/issue/KT-12440) Removing unused parameter results in Exception "Refactorings should not be started inside write action"
- [`KT-12006`](https://youtrack.jetbrains.com/issue/KT-12006) getLanguageLevel is slow for Kotlin light classes
- [`KT-12026`](https://youtrack.jetbrains.com/issue/KT-12026) "Constant expression required" in Java for const Kotlin values
- [`KT-12259`](https://youtrack.jetbrains.com/issue/KT-12259) ClassCastException in light classes while trying to create generic property
- [`KT-12289`](https://youtrack.jetbrains.com/issue/KT-12289) Remove unnecessary `?` from `serr` live template
- [`KT-12110`](https://youtrack.jetbrains.com/issue/KT-12110) Map help button of the Compiler - Kotlin page
- [`KT-12075`](https://youtrack.jetbrains.com/issue/KT-12075) Kotlin Plugin update check: make dumbaware
- [`KT-10255`](https://youtrack.jetbrains.com/issue/KT-10255) call BuildManager.clearState(project) in apply() method of Kotlin Compiler Settings configurable
- [`KT-11841`](https://youtrack.jetbrains.com/issue/KT-11841) New Project / Module wizard, Gradle: pure Kotlin module is created without `repositories` call in build.gradle
- [`KT-11095`](https://youtrack.jetbrains.com/issue/KT-11095) Java cannot infer generic return type of Kotlin function (with java 8 language level)
- [`KT-12090`](https://youtrack.jetbrains.com/issue/KT-12090) Intellij/Kotlin plugin does not handle generic return type of static method defined in Kotlin, called from Java
- [`KT-12206`](https://youtrack.jetbrains.com/issue/KT-12206) Fix NoSuchFieldError on accessing base property without backing field in evaluate expression
- [`KT-12516`](https://youtrack.jetbrains.com/issue/KT-12516) File Structure: Kotlin annotation classes have Java annotation icons
- [`KT-11328`](https://youtrack.jetbrains.com/issue/KT-11328) "New Kotlin class": generates packages when fully qualified name is specified
- [`KT-11778`](https://youtrack.jetbrains.com/issue/KT-11778) Exception in Lombok plugin: Rewrite at slice FUNCTION
- [`KT-11708`](https://youtrack.jetbrains.com/issue/KT-11708) "Go to declaration" doesn't work on a call to function with SAM conversion on a derived type
- [`KT-12381`](https://youtrack.jetbrains.com/issue/KT-12381) Prefer not-nullable return type when overriding Java method without nullability annotation
- [`KT-12647`](https://youtrack.jetbrains.com/issue/KT-12647) Performance improvement for test-related line markers
- [`KT-12526`](https://youtrack.jetbrains.com/issue/KT-12526) Kotlin intentions increase PSI modification counts from isAvailable, even in daemon threads

### Reflection

###### Issues fixed
- [`KT-11531`](https://youtrack.jetbrains.com/issue/KT-11531) Optimize "KCallable.name"
- [`KT-10771`](https://youtrack.jetbrains.com/issue/KT-10771) Reflection on Function objects does not support lambdas with generic return type
- [`KT-11824`](https://youtrack.jetbrains.com/issue/KT-11824) Reflection inconsistency between member property and accessor

### JS

- Improve performance of maps and sets

###### Issues fixed
- [`KT-6942`](https://youtrack.jetbrains.com/issue/KT-6942) Generate structural equality check (i.e. `Any.equals`) instead of referential check (===) value equality patterns in `when`
- [`KT-7228`](https://youtrack.jetbrains.com/issue/KT-7228) Wrong AbstractList signature
- [`KT-8299`](https://youtrack.jetbrains.com/issue/KT-8299) Wrong access to private member in autogenerated code in data class
- [`KT-11346`](https://youtrack.jetbrains.com/issue/KT-11346) Reified functions like `filterIsInstance` are now available in JS Standard Library
- [`KT-12305`](https://youtrack.jetbrains.com/issue/KT-12305) Incorrect translation of `vararg` in `@native` functions
- [`KT-12254`](https://youtrack.jetbrains.com/issue/KT-12254) JsEmptyExpression in initializer when compiling code like `val x = throw Exception()`
- [`KT-11960`](https://youtrack.jetbrains.com/issue/KT-11960) Wrong code generated when a method of a local class calls constructor of the class
- [`KT-10931`](https://youtrack.jetbrains.com/issue/KT-10931) Incorrect inlining of library method with optional parameters
- [`KT-12417`](https://youtrack.jetbrains.com/issue/KT-12417) Wrong check cast generated for KMutableProperty

### Tools

###### New features

- [`KT-11839`](https://youtrack.jetbrains.com/issue/KT-11839) Maven goal to execute kotlin script

###### Issues fixed

- KAPT: fix error when using enum constructors with parameters
- Various problems with gradle 2.2 fixed: [`KT-12478`](https://youtrack.jetbrains.com/issue/KT-12478), [`KT-12406`](https://youtrack.jetbrains.com/issue/KT-12406), [`KT-12478`](https://youtrack.jetbrains.com/issue/KT-12478)
- [`KT-12595`](https://youtrack.jetbrains.com/issue/KT-12595) JPS: Fixed com.intellij.util.io.MappingFailedException: Cannot map buffer
- [`KT-11166`](https://youtrack.jetbrains.com/issue/KT-11166) Gradle: Unable to access internal classes from test code within the same module
- [`KT-12352`](https://youtrack.jetbrains.com/issue/KT-12352) KAPT: Fix "Classpath entry points to a non-existent location" warnings
- [`KT-12074`](https://youtrack.jetbrains.com/issue/KT-12074) Building Kotlin maven projects using a parent pom will silently fail
- [`KT-11770`](https://youtrack.jetbrains.com/issue/KT-11770) Warning "RuntimeException: Could not find installation home path" when using Gradle Incremental Compilation
- [`KT-10969`](https://youtrack.jetbrains.com/issue/KT-10969) Android extensions: NullPointerException when finding view in Fragment
- [`KT-11885`](https://youtrack.jetbrains.com/issue/KT-11885) Gradle/Android: Unresolved reference "kotlinx" when classpath dependency is defined in root build.gradle
- [`KT-12786`](https://youtrack.jetbrains.com/issue/KT-12786) Deprecation warning with Gradle 2.14

## 1.0.2-1

- [KT-12159](https://youtrack.jetbrains.com/issue/KT-12159), [KT-12406](https://youtrack.jetbrains.com/issue/KT-12406), [KT-12431](https://youtrack.jetbrains.com/issue/KT-12431), [KT-12478](https://youtrack.jetbrains.com/issue/KT-12478) Support Android Studio 2.2
- [KT-11770](https://youtrack.jetbrains.com/issue/KT-11770) Fix warning "RuntimeException: Could not find installation home path" when using incremental compilation in Gradle
- [KT-12436](https://youtrack.jetbrains.com/issue/KT-12436), [KT-12440](https://youtrack.jetbrains.com/issue/KT-12440) Fix multiple exceptions during refactorings in IDEA 2016.2 EAP
- [KT-12015](https://youtrack.jetbrains.com/issue/KT-12015), [KT-12047](https://youtrack.jetbrains.com/issue/KT-12047), [KT-12387](https://youtrack.jetbrains.com/issue/KT-12387) Fix multiple issues in Kotlin Lint checks

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
- [KT-5717](https://youtrack.jetbrains.com/issue/KT-5717) "Replace `when` with `if`": do not lose comments
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
