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

## Previous releases

This release also includes the fixes and improvements from the previous
[`1.1.2`](https://github.com/JetBrains/kotlin/blob/1.1.2/ChangeLog.md) release.