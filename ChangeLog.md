# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->


## 1.1.3-3

- [`KT-18954`](https://youtrack.jetbrains.com/issue/KT-18954) Fixed exception in Kotlin plugin updater

## 1.1.3-2

#### Fixes

-   Noarg compiler plugin reverted to 1.1.2 behavior: by default, it will not run any initialization code
    from the generated default constructor. If you want to run initializers, you need to enable
    the corresponding option as described in the documentation.
    Note that if a @noarg class has initializers that depend on constructor parameters, you will get incorrect
    compiler behavior, so you shouldn't enable this option if you have such classes in your project.
    This resolves [`KT-18667`](https://youtrack.jetbrains.com/issue/KT-18667) and [`KT-18668`](https://youtrack.jetbrains.com/issue/KT-18668).
- [`KT-18689`](https://youtrack.jetbrains.com/issue/KT-18689) Incorrect bytecode generated when passing a bound member reference to an inline function with default argument values
- [`KT-18377`](https://youtrack.jetbrains.com/issue/KT-18377) Syntax error while generating kapt stubs
- [`KT-18411`](https://youtrack.jetbrains.com/issue/KT-18411) Slow debugger step-in into inlined function
- [`KT-18687`](https://youtrack.jetbrains.com/issue/KT-18687) Deadlock in resolve with Kotlin 1.1.3
- [`KT-18726`](https://youtrack.jetbrains.com/issue/KT-18726) Frequent UI hangs in 2017.2 EAPs

## 1.1.3

### Android

#### New Features

- [`KT-12049`](https://youtrack.jetbrains.com/issue/KT-12049) Kotlin Lint: "Missing Parcelable CREATOR field" could suggest "Add implementation" quick fix
- [`KT-16712`](https://youtrack.jetbrains.com/issue/KT-16712) Show warning in IDEA when using Java 1.8 api in Android
- [`KT-16843`](https://youtrack.jetbrains.com/issue/KT-16843) Android: provide gutter icons for resources like colors and drawables
- [`KT-17389`](https://youtrack.jetbrains.com/issue/KT-17389) Implement Intention "Add Activity / BroadcastReceiver / Service to manifest" 
- [`KT-17465`](https://youtrack.jetbrains.com/issue/KT-17465) Add intentions Add/Remove/Redo parcelable implementation
#### Fixes

- [`KT-14970`](https://youtrack.jetbrains.com/issue/KT-14970) ClassCastException: butterknife.lint.LintRegistry cannot be cast to com.android.tools.klint.client.api.IssueRegistry
- [`KT-17287`](https://youtrack.jetbrains.com/issue/KT-17287) Configure kotlin in Android Studio: don't show menu Choose Configurator with single choice
- [`KT-17288`](https://youtrack.jetbrains.com/issue/KT-17288) Android Studio: please remove menu item Configure Kotlin (JavaScript) in Project
- [`KT-17289`](https://youtrack.jetbrains.com/issue/KT-17289) Android Studio: Hide "Configure Kotlin" balloon if Kotlin is configured from a kt file banner
- [`KT-17291`](https://youtrack.jetbrains.com/issue/KT-17291) Android Studio 2.4: Cannot get property 'metaClass' on null object error after Kotlin configuring
- [`KT-17610`](https://youtrack.jetbrains.com/issue/KT-17610) "Unknown reference: kotlinx"

### Compiler

#### New Features

- [`KT-11167`](https://youtrack.jetbrains.com/issue/KT-11167) Support compilation against JRE 9
- [`KT-17497`](https://youtrack.jetbrains.com/issue/KT-17497) Warn about redundant else branch in exhaustive when
#### Performance Improvements

- [`KT-7931`](https://youtrack.jetbrains.com/issue/KT-7931) Optimize iteration over strings/charsequences on JVM
- [`KT-10848`](https://youtrack.jetbrains.com/issue/KT-10848) Optimize substitution of inline function with default parameters
- [`KT-12497`](https://youtrack.jetbrains.com/issue/KT-12497) Optimize inlined bytecode for functions with default parameters
- [`KT-17342`](https://youtrack.jetbrains.com/issue/KT-17342) Optimize control-flow for case of many variables
- [`KT-17562`](https://youtrack.jetbrains.com/issue/KT-17562) Optimize KtFile::isScript
#### Fixes

- [`KT-4960`](https://youtrack.jetbrains.com/issue/KT-4960) Redeclaration is not reported for type parameters of interfaces
- [`KT-5160`](https://youtrack.jetbrains.com/issue/KT-5160) No warning when a lambda parameter hides a variable
- [`KT-5246`](https://youtrack.jetbrains.com/issue/KT-5246) is check fails on cyclic type parameter bounds
- [`KT-5354`](https://youtrack.jetbrains.com/issue/KT-5354) Wrong label resolution when label name clash with fun name
- [`KT-7645`](https://youtrack.jetbrains.com/issue/KT-7645) Prohibit default value for `catch`-block parameter
- [`KT-7724`](https://youtrack.jetbrains.com/issue/KT-7724) Can never import private member 
- [`KT-7796`](https://youtrack.jetbrains.com/issue/KT-7796) Wrong scope for default parameter value resolution
- [`KT-7984`](https://youtrack.jetbrains.com/issue/KT-7984) Unexpected "Unresolved reference" in a default value expression in a local function
- [`KT-7985`](https://youtrack.jetbrains.com/issue/KT-7985) Unexpected "Unresolved reference to a type parameter" in a default value expression in a local function
- [`KT-8320`](https://youtrack.jetbrains.com/issue/KT-8320) It should not be possible to catch a type parameter type
- [`KT-8877`](https://youtrack.jetbrains.com/issue/KT-8877) Automatic labeling doesn't work for infix calls
- [`KT-9251`](https://youtrack.jetbrains.com/issue/KT-9251) Qualified this does not work with labeled function literals
- [`KT-9551`](https://youtrack.jetbrains.com/issue/KT-9551) False warning "No cast needed"
- [`KT-9645`](https://youtrack.jetbrains.com/issue/KT-9645) Incorrect inspection: No cast Needed
- [`KT-9986`](https://youtrack.jetbrains.com/issue/KT-9986) 'null as T' should be unchecked cast
- [`KT-10397`](https://youtrack.jetbrains.com/issue/KT-10397) java.lang.reflect.GenericSignatureFormatError when generic inner class is mentioned in function signature
- [`KT-11474`](https://youtrack.jetbrains.com/issue/KT-11474) ISE: Requested A, got foo.A in JavaClassFinderImpl on Java file with package not matching directory
- [`KT-11622`](https://youtrack.jetbrains.com/issue/KT-11622) False "No cast needed" when ambiguous call because of smart cast
- [`KT-12245`](https://youtrack.jetbrains.com/issue/KT-12245) Code with annotation that has an unresolved identifier as a parameter compiles successfully
- [`KT-12269`](https://youtrack.jetbrains.com/issue/KT-12269) False "Non-null type is checked for instance of nullable type"
- [`KT-12683`](https://youtrack.jetbrains.com/issue/KT-12683) A problem with `is` operator and non-reified type-parameters
- [`KT-12690`](https://youtrack.jetbrains.com/issue/KT-12690) USELESS_CAST compiler warning may break code when fix is applied
- [`KT-13348`](https://youtrack.jetbrains.com/issue/KT-13348) Report useless cast on safe cast from nullable type to the same not null type
- [`KT-13597`](https://youtrack.jetbrains.com/issue/KT-13597) No check for accessing final field in local object in constructor
- [`KT-13997`](https://youtrack.jetbrains.com/issue/KT-13997) Incorrect "Property must be initialized or be abstract" error for property with external accessors
- [`KT-14381`](https://youtrack.jetbrains.com/issue/KT-14381) Possible val reassignment not detected inside init block
- [`KT-14564`](https://youtrack.jetbrains.com/issue/KT-14564) java.lang.VerifyError: Bad local variable type
- [`KT-14801`](https://youtrack.jetbrains.com/issue/KT-14801) Invoke error message if nested class has the same name as a function from base class
- [`KT-14977`](https://youtrack.jetbrains.com/issue/KT-14977) IDE doesn't warn about checking null value of variable that cannot be null
- [`KT-15085`](https://youtrack.jetbrains.com/issue/KT-15085) Label and function naming conflict is resolved in unintuitive way
- [`KT-15161`](https://youtrack.jetbrains.com/issue/KT-15161) False warning "no cast needed" for array creation
- [`KT-15480`](https://youtrack.jetbrains.com/issue/KT-15480) Cannot destruct a list when "if" has an "else" branch
- [`KT-15495`](https://youtrack.jetbrains.com/issue/KT-15495) Internal typealiases in the same module are inaccessible on incremental compilation
- [`KT-15566`](https://youtrack.jetbrains.com/issue/KT-15566) Object member imported in file scope used in delegation expression in object declaration should be a compiler error
- [`KT-16016`](https://youtrack.jetbrains.com/issue/KT-16016) Compiler failure with NO_EXPECTED_TYPE
- [`KT-16426`](https://youtrack.jetbrains.com/issue/KT-16426) Return statement resolved to function instead of property getter
- [`KT-16813`](https://youtrack.jetbrains.com/issue/KT-16813) Anonymous objects returned from private-in-file members should behave as for private class members
- [`KT-16864`](https://youtrack.jetbrains.com/issue/KT-16864) Local delegate + ad-hoc object leads to CCE
- [`KT-17144`](https://youtrack.jetbrains.com/issue/KT-17144) Breakpoint inside `when`
- [`KT-17149`](https://youtrack.jetbrains.com/issue/KT-17149) Incorrect warning "Kotlin: This operation has led to an overflow"
- [`KT-17156`](https://youtrack.jetbrains.com/issue/KT-17156) No re-parse after lambda was converted to block
- [`KT-17318`](https://youtrack.jetbrains.com/issue/KT-17318) Typo in DSL Marker message `cant`
- [`KT-17384`](https://youtrack.jetbrains.com/issue/KT-17384) break/continue expression in inlined function parameter argument causes compilation exception
- [`KT-17457`](https://youtrack.jetbrains.com/issue/KT-17457) Suspend + LongRange couldn't transform method node issue in Kotlin 1.1.1
- [`KT-17479`](https://youtrack.jetbrains.com/issue/KT-17479) val reassign is allowed via explicit this receiver
- [`KT-17560`](https://youtrack.jetbrains.com/issue/KT-17560) Overload resolution ambiguity on semi-valid class-files generated by Scala
- [`KT-17572`](https://youtrack.jetbrains.com/issue/KT-17572) try-catch expression in inlined function parameter argument causes compilation exception
- [`KT-17573`](https://youtrack.jetbrains.com/issue/KT-17573) try-finally expression in inlined function parameter argument fails with VerifyError
- [`KT-17588`](https://youtrack.jetbrains.com/issue/KT-17588) Compiler error while optimizer tries to get rid of captured variable
- [`KT-17590`](https://youtrack.jetbrains.com/issue/KT-17590) conditional return in inline function parameter argument causes compilation exception
- [`KT-17591`](https://youtrack.jetbrains.com/issue/KT-17591) non-conditional return in inline function parameter argument causes compilation exception
- [`KT-17613`](https://youtrack.jetbrains.com/issue/KT-17613) 'this' expression referring to deprecated class instance is highlighted as deprecated in IDE
- [`KT-18358`](https://youtrack.jetbrains.com/issue/KT-18358) Keep smart pointers instead of PSI elements in JavaElementImpl and its descendants

### IDE

#### New Features

- [`KT-7810`](https://youtrack.jetbrains.com/issue/KT-7810) Separate icon for abstract class
- [`KT-8617`](https://youtrack.jetbrains.com/issue/KT-8617) Recognize TODO method usages and highlight them same as TODO-comment
- [`KT-12629`](https://youtrack.jetbrains.com/issue/KT-12629) Add rainbow/semantic-highlighting for local variables
- [`KT-14109`](https://youtrack.jetbrains.com/issue/KT-14109) support parameter hints in idea plugin
- [`KT-16645`](https://youtrack.jetbrains.com/issue/KT-16645) Support inlay type hints for implicitly typed vals, properties, and functions
- [`KT-17807`](https://youtrack.jetbrains.com/issue/KT-17807) Add Smart Enter processor for object expessions
#### Performance Improvements

- [`KT-16995`](https://youtrack.jetbrains.com/issue/KT-16995) Typing during in-place refactorings is impossibly laggy
- [`KT-17331`](https://youtrack.jetbrains.com/issue/KT-17331) Frequent long editor freezes
- [`KT-17383`](https://youtrack.jetbrains.com/issue/KT-17383) Slow editing in Kotlin files If breadcrumbs are enabled in module with many dependencies
- [`KT-17495`](https://youtrack.jetbrains.com/issue/KT-17495) Much time spent in LibraryDependenciesCache.getLibrariesAndSdksUsedWith
#### Fixes

- [`KT-7848`](https://youtrack.jetbrains.com/issue/KT-7848) When you paste text into a string literal special symbols should be escaped
- [`KT-7954`](https://youtrack.jetbrains.com/issue/KT-7954) 'Go to symbol' doesn't show containing declaration for local symbols
- [`KT-9091`](https://youtrack.jetbrains.com/issue/KT-9091) Sometimes backticks of the method name with spaces are highlighted with rose background
- [`KT-10577`](https://youtrack.jetbrains.com/issue/KT-10577) Refactor / Move Kotlin + Java files adds wrong import in very specific case
- [`KT-12856`](https://youtrack.jetbrains.com/issue/KT-12856) Import fold region is not updated to include imports added while editing file
- [`KT-14161`](https://youtrack.jetbrains.com/issue/KT-14161) Navigate to symbol doesn't see local named functions
- [`KT-14601`](https://youtrack.jetbrains.com/issue/KT-14601) Formatter inserts unnecessary indent before 'else'
- [`KT-14639`](https://youtrack.jetbrains.com/issue/KT-14639) Incorrect name of code style setting: Align in columns 'case' branches
- [`KT-15029`](https://youtrack.jetbrains.com/issue/KT-15029) "Go to symbol" action doesn't find properties declared in primary constructors
- [`KT-15255`](https://youtrack.jetbrains.com/issue/KT-15255) Move cursor to a better place when creating a new Kotlin file
- [`KT-15273`](https://youtrack.jetbrains.com/issue/KT-15273) Kotlin IDE plugin adds `import java.lang.String` with "Optimize Imports", making project broken
- [`KT-16159`](https://youtrack.jetbrains.com/issue/KT-16159) Wrong "Constructor call" highlighting if operator is called on newly created object
- [`KT-16392`](https://youtrack.jetbrains.com/issue/KT-16392) Gradle/Maven java module: Add framework support/ Kotlin (Java or JavaScript) adds nothing
- [`KT-16423`](https://youtrack.jetbrains.com/issue/KT-16423) Show expression type doesn't work when selecting from the middle of expression with "Expand Selection"
- [`KT-16635`](https://youtrack.jetbrains.com/issue/KT-16635) Do not show kotlin-specific live templates macros for all context types
- [`KT-16755`](https://youtrack.jetbrains.com/issue/KT-16755) No "Is sublassed by" icon for sealed class
- [`KT-16775`](https://youtrack.jetbrains.com/issue/KT-16775) Rewrite at slice CLASS key: OBJECT_DECLARATION while writing code in IDE
- [`KT-16803`](https://youtrack.jetbrains.com/issue/KT-16803) Suspending iteration is not marked in the gutter by IDEA as suspending invocation
- [`KT-17037`](https://youtrack.jetbrains.com/issue/KT-17037) Editor suggests to import `EmptyCoroutineContext.plus` for any unresolved `+`
- [`KT-17046`](https://youtrack.jetbrains.com/issue/KT-17046) Kotlin facet, Compiler plugins: last line is shown empty when not selected
- [`KT-17088`](https://youtrack.jetbrains.com/issue/KT-17088) Settings: Kotlin Compiler: "Destination directory" should be enabled if "Copy library runtime files" is on on the dialog opening 
- [`KT-17094`](https://youtrack.jetbrains.com/issue/KT-17094) Kotlin facet, additional command line parameters dialog: please provide a title
- [`KT-17138`](https://youtrack.jetbrains.com/issue/KT-17138) Configure Kotlin in Project: Choose Configurator popup: names could be unified
- [`KT-17145`](https://youtrack.jetbrains.com/issue/KT-17145) Kotlin facet: IllegalArgumentException on attempt to show settings common for several javascript kotlin facets with different moduleKind
- [`KT-17223`](https://youtrack.jetbrains.com/issue/KT-17223) Absolute path to Kotlin compiler plugin in IML
- [`KT-17293`](https://youtrack.jetbrains.com/issue/KT-17293) Project Structure dialog is opened too slow for a project with a lot of empty gradle modules
- [`KT-17304`](https://youtrack.jetbrains.com/issue/KT-17304) IDEA shows wrong type for expressions
- [`KT-17439`](https://youtrack.jetbrains.com/issue/KT-17439) Kotlin: 'autoscroll from source' doesn't work in Structure view
- [`KT-17448`](https://youtrack.jetbrains.com/issue/KT-17448) Regression: Sample Resolve 
- [`KT-17482`](https://youtrack.jetbrains.com/issue/KT-17482) Set jvmTarget to 1.8 by default when configuring a project with JDK 1.8
- [`KT-17492`](https://youtrack.jetbrains.com/issue/KT-17492) -jvm-target is ignored by IntelliJ
- [`KT-17505`](https://youtrack.jetbrains.com/issue/KT-17505) LazyLightClassMemberMatchingError from collection implementation
- [`KT-17517`](https://youtrack.jetbrains.com/issue/KT-17517) Compiler options specified as properties are not handled by Maven importer
- [`KT-17521`](https://youtrack.jetbrains.com/issue/KT-17521) Quickfix to enable coroutines should work for Maven projects
- [`KT-17525`](https://youtrack.jetbrains.com/issue/KT-17525) IDE: KNPE at KotlinAddImportActionKt.createSingleImportActionForConstructor() on invalid reference to inner class constructor
- [`KT-17578`](https://youtrack.jetbrains.com/issue/KT-17578) Throwable: "Reported element PsiIdentifier:AnnotationConfiguration is not from the file 'PsiFile:InSource.kt' the inspection 'ImplicitSubclassInspection'"
- [`KT-17638`](https://youtrack.jetbrains.com/issue/KT-17638) ISE in KotlinElementDescriptionProvider.renderShort
- [`KT-17698`](https://youtrack.jetbrains.com/issue/KT-17698) Unknown library format - prevents IDEA from configuring Kotlin JS
- [`KT-17714`](https://youtrack.jetbrains.com/issue/KT-17714) UAST inspection on non-physical element
- [`KT-17722`](https://youtrack.jetbrains.com/issue/KT-17722) IntelliJ plugin uses wrong JVM target when Kotlin Facet is not configured
- [`KT-17770`](https://youtrack.jetbrains.com/issue/KT-17770) Kotlin IntelliJ plugin fails to re-index Gradle script classpath after change to the `plugins` block
- [`KT-17777`](https://youtrack.jetbrains.com/issue/KT-17777) Logger$EmptyThrowable: "Facet Kotlin (app) [kotlin-language] not found" at FacetModelImpl.removeFacet()
- [`KT-17810`](https://youtrack.jetbrains.com/issue/KT-17810) Exception from unused import inspection leads to code analysis hangs
- [`KT-17821`](https://youtrack.jetbrains.com/issue/KT-17821) In Kotlin's plugin KotlinJsMetadataVersionIndex loads file with VfsUtilCore.loadText
- [`KT-17840`](https://youtrack.jetbrains.com/issue/KT-17840) Show expression type on `this` shows bogus disambiguation
- [`KT-17845`](https://youtrack.jetbrains.com/issue/KT-17845) Searching for usages of override property in primary constructor doesn't suggest base property search
- [`KT-17847`](https://youtrack.jetbrains.com/issue/KT-17847) Kotlin facet: strange warning if API version = 1.2
- [`KT-17857`](https://youtrack.jetbrains.com/issue/KT-17857) Java should see classes affected by "allopen" plugin as open
- [`KT-17861`](https://youtrack.jetbrains.com/issue/KT-17861) Setting 'kotlin.experimental.coroutines "enable"' doesn't work for Android projects
- [`KT-17875`](https://youtrack.jetbrains.com/issue/KT-17875) New Project/Module with Kotlin: on attempt to use libraries from plugin IDE suggests to rewrite them
- [`KT-17876`](https://youtrack.jetbrains.com/issue/KT-17876) New Project/Module with Kotlin: with "Copy to" option only part of jars are copied
- [`KT-17899`](https://youtrack.jetbrains.com/issue/KT-17899) Navigate to symbol: vararg signatures are indistinguishable from non-vararg ones
- [`KT-18070`](https://youtrack.jetbrains.com/issue/KT-18070) KtLightModifierList.hasExplicitModifier("default") is true for interface method with body

### IDE. Completion

#### New Features

- [`KT-11250`](https://youtrack.jetbrains.com/issue/KT-11250) Auto-completion for convention function names in 'operator fun' definitions
- [`KT-12293`](https://youtrack.jetbrains.com/issue/KT-12293) Autocompletion should propose `lateinit var` in addition to `lateinit`
- [`KT-13673`](https://youtrack.jetbrains.com/issue/KT-13673) Add 'companion { ... }'  code completion opsion
#### Performance Improvements

- [`KT-10978`](https://youtrack.jetbrains.com/issue/KT-10978) Kotlin + JOOQ + Intellij performance is unusable 
- [`KT-16715`](https://youtrack.jetbrains.com/issue/KT-16715) Typing is very slow since 1.1
- [`KT-16850`](https://youtrack.jetbrains.com/issue/KT-16850) UI freeze for several seconds during inserting selected completion variant
#### Fixes

- [`KT-13524`](https://youtrack.jetbrains.com/issue/KT-13524) Completing the keyword 'constructor' before a primary constructor wrongly inserts parentheses
- [`KT-14665`](https://youtrack.jetbrains.com/issue/KT-14665) No completion for "else" keyword
- [`KT-15603`](https://youtrack.jetbrains.com/issue/KT-15603) Annoying completion when making a primary constructor private
- [`KT-16161`](https://youtrack.jetbrains.com/issue/KT-16161) Completion of 'onEach' inserts unneeded angular brackets
- [`KT-16856`](https://youtrack.jetbrains.com/issue/KT-16856) Code completion optimization

### IDE. Debugger

- [`KT-15823`](https://youtrack.jetbrains.com/issue/KT-15823) Breakpoints not work inside crossinline from init of object passed into collection
- [`KT-15854`](https://youtrack.jetbrains.com/issue/KT-15854) Debugger not able to evaluate internal member functions
- [`KT-16025`](https://youtrack.jetbrains.com/issue/KT-16025) Step into suspend functions stops at the function end
- [`KT-17295`](https://youtrack.jetbrains.com/issue/KT-17295) Can't stop in kotlin.concurrent.timer lambda parameter

### IDE. Inspections and Intentions

#### New Features

- [`KT-10981`](https://youtrack.jetbrains.com/issue/KT-10981) Quickfix for INAPPLICABLE_JVM_FIELD to replace with 'const' when possible
- [`KT-14046`](https://youtrack.jetbrains.com/issue/KT-14046) Add intention to add inline keyword if a function has parameter with noinline and/or crossinline modifier
- [`KT-14137`](https://youtrack.jetbrains.com/issue/KT-14137) Add intention to convert top level val with object expression to object
- [`KT-15903`](https://youtrack.jetbrains.com/issue/KT-15903) QuickFix to add/remove suspend in hierarchies
- [`KT-16786`](https://youtrack.jetbrains.com/issue/KT-16786) Intention to add "open" modifier to a non-private method or property in an open class
- [`KT-16851`](https://youtrack.jetbrains.com/issue/KT-16851) Quickfix adding qualifier `@call` to unallowed 'return' in closures
- [`KT-17053`](https://youtrack.jetbrains.com/issue/KT-17053) Inspection to detect use of callable reference as a lambda body
- [`KT-17054`](https://youtrack.jetbrains.com/issue/KT-17054) Intention/ inspection to convert 'if' with 'is' check to 'as?' with safe call 
- [`KT-17191`](https://youtrack.jetbrains.com/issue/KT-17191) Intention to name anonymous (_) parameter
- [`KT-17221`](https://youtrack.jetbrains.com/issue/KT-17221) Inspection for recursive calls in property accessors
- [`KT-17520`](https://youtrack.jetbrains.com/issue/KT-17520) Quickfix to update language/API version should work for Maven projects
- [`KT-17650`](https://youtrack.jetbrains.com/issue/KT-17650) Add quickfix inserting 'lateinit' modifier for not-initialized property
- [`KT-17660`](https://youtrack.jetbrains.com/issue/KT-17660) Inspection: data class copy without named argument(s)
#### Fixes

- [`KT-10211`](https://youtrack.jetbrains.com/issue/KT-10211) "Replace infix call with ordinary call" appears both as a quickfix and as an intention in the pop-up
- [`KT-11003`](https://youtrack.jetbrains.com/issue/KT-11003) Invalid quickfix in companion object for open properties
- [`KT-12805`](https://youtrack.jetbrains.com/issue/KT-12805) False positive redundant semicolon after while without block expression
- [`KT-14335`](https://youtrack.jetbrains.com/issue/KT-14335) Unexpected range of "convert lambda to reference" intention
- [`KT-14435`](https://youtrack.jetbrains.com/issue/KT-14435) "Use destructuring declaration" should be available as intention even without usages
- [`KT-14443`](https://youtrack.jetbrains.com/issue/KT-14443) IDEA intention suggest to make a method in an interface final
- [`KT-14820`](https://youtrack.jetbrains.com/issue/KT-14820) Convert function to property shouldn't insert explicit type if it was inferred previously
- [`KT-15076`](https://youtrack.jetbrains.com/issue/KT-15076) Replace if with elvis inspection should not be reported in some complex cases
- [`KT-15543`](https://youtrack.jetbrains.com/issue/KT-15543) "Convert receiver to parameter" refactoring breaks code
- [`KT-15942`](https://youtrack.jetbrains.com/issue/KT-15942) "Convert to secondary constructor" intention is available for data class
- [`KT-16136`](https://youtrack.jetbrains.com/issue/KT-16136) Wrong type parameter variance suggested if type parameter is used in nested anonymous object
- [`KT-16339`](https://youtrack.jetbrains.com/issue/KT-16339) Incorrect warning: 'protected' visibility is effectively 'private' in a final class
- [`KT-16577`](https://youtrack.jetbrains.com/issue/KT-16577) "Redundant semicolon" is not reported for semicolon after package statement in file with no imports
- [`KT-17079`](https://youtrack.jetbrains.com/issue/KT-17079) Kotlin: Bad conversion of double comparison to range check if bounds have mixed types
- [`KT-17372`](https://youtrack.jetbrains.com/issue/KT-17372) Specify explicit lambda signature handles anonymous parameters incorrectly
- [`KT-17404`](https://youtrack.jetbrains.com/issue/KT-17404) Editor: attempt to pass type parameter as reified argument causes AE "Classifier descriptor of a type should be of type ClassDescriptor" at DescriptorUtils.getClassDescriptorForTypeConstructor()
- [`KT-17408`](https://youtrack.jetbrains.com/issue/KT-17408) "Convert to secondary constructor" intention is available for annotation parameters
- [`KT-17503`](https://youtrack.jetbrains.com/issue/KT-17503) Intention "To raw string literal" should handle string concatenations
- [`KT-17599`](https://youtrack.jetbrains.com/issue/KT-17599) "Make primary constructor internal" intention is available for annotation class 
- [`KT-17600`](https://youtrack.jetbrains.com/issue/KT-17600) "Make primary constructor private" intention is available for annotation class
- [`KT-17707`](https://youtrack.jetbrains.com/issue/KT-17707) "Final declaration can't be overridden at runtime" inspection reports Kotlin classes non final due to compiler plugin
- [`KT-17708`](https://youtrack.jetbrains.com/issue/KT-17708) "Move to class body" intention is available for annotation parameters
- [`KT-17762`](https://youtrack.jetbrains.com/issue/KT-17762) 'Convert to range' intention generates inequivalent code for doubles

### IDE. Refactorings

#### Performance Improvements

- [`KT-17234`](https://youtrack.jetbrains.com/issue/KT-17234) Refactor / Inline on library property is rejected after GUI freeze for a while
- [`KT-17333`](https://youtrack.jetbrains.com/issue/KT-17333) KotlinChangeInfo retains 132MB of the heap
#### Fixes

- [`KT-8370`](https://youtrack.jetbrains.com/issue/KT-8370) "Can't move to original file" should not be an error
- [`KT-8930`](https://youtrack.jetbrains.com/issue/KT-8930) Refactor / Move preivew: moved element is shown as reference, and its file as subject
- [`KT-9158`](https://youtrack.jetbrains.com/issue/KT-9158) Refactor / Move preview mentions the package statement of moved class as a usage
- [`KT-13192`](https://youtrack.jetbrains.com/issue/KT-13192) Refactor / Move: to another class: "To" field code completion suggests facade and Java classes
- [`KT-13466`](https://youtrack.jetbrains.com/issue/KT-13466) Refactor / Move: class to upper level: the package statement is not updated
- [`KT-15519`](https://youtrack.jetbrains.com/issue/KT-15519) KDoc comments for data class values get removed by Change Signature
- [`KT-17211`](https://youtrack.jetbrains.com/issue/KT-17211) Refactor / Move several files: superfluous FQN is inserted into reference to same file's element
- [`KT-17213`](https://youtrack.jetbrains.com/issue/KT-17213) Refactor / Inline Function: parameters of lambda as call argument turn incompilable
- [`KT-17272`](https://youtrack.jetbrains.com/issue/KT-17272) Refactor / Inline Function: unused String literal in parameters is kept (while unsed Int is not)
- [`KT-17273`](https://youtrack.jetbrains.com/issue/KT-17273) Refactor / Inline Function: PIEAE: "Element: class org.jetbrains.kotlin.psi.KtCallExpression because: different providers" at PsiUtilCore.ensureValid()
- [`KT-17296`](https://youtrack.jetbrains.com/issue/KT-17296) Refactor / Inline Function: UOE at ExpressionReplacementPerformer.findOrCreateBlockToInsertStatement() for call of multi-statement function in declaration
- [`KT-17330`](https://youtrack.jetbrains.com/issue/KT-17330) Inline kotlin function causes an infinite loop
- [`KT-17395`](https://youtrack.jetbrains.com/issue/KT-17395) Refactor / Inline Function: arguments passed to lambda turns code to incompilable
- [`KT-17496`](https://youtrack.jetbrains.com/issue/KT-17496) Refactor / Move: calls to moved extension function type properties are updated (incorrectly)
- [`KT-17515`](https://youtrack.jetbrains.com/issue/KT-17515) Refactor / Move inner class to another class, Move companion object: disabled in editor, but available in Move dialog
- [`KT-17526`](https://youtrack.jetbrains.com/issue/KT-17526) Refactor / Move: reference to companion member gets superfluous companion name in certain cases
- [`KT-17538`](https://youtrack.jetbrains.com/issue/KT-17538) Refactor / Move: moving file with import alias removes alias usage from code
- [`KT-17545`](https://youtrack.jetbrains.com/issue/KT-17545) Refactor / Move: false Problems Detected on moving class using parent's protected class, object
- [`KT-18018`](https://youtrack.jetbrains.com/issue/KT-18018) F5 (for Copy) does not work for Kotlin files anymore
- [`KT-18205`](https://youtrack.jetbrains.com/issue/KT-18205) Moving multiple classes causes imports to be converted to fully qualified class names

### Infrastructure

- [`KT-14988`](https://youtrack.jetbrains.com/issue/KT-14988) Support running the Kotlin compiler on Java 9
- [`KT-17112`](https://youtrack.jetbrains.com/issue/KT-17112) IncompatibleClassChangeError on invoking Kotlin compiler daemon on JDK 9

### JavaScript

#### Fixes

- [`KT-12926`](https://youtrack.jetbrains.com/issue/KT-12926) JS: use # instead of @ when linking to sourcemap from generated code
- [`KT-13577`](https://youtrack.jetbrains.com/issue/KT-13577) Double.hashCode is zero for big numbers
- [`KT-15135`](https://youtrack.jetbrains.com/issue/KT-15135) JS: support friend modules
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
- [`KT-17700`](https://youtrack.jetbrains.com/issue/KT-17700) Wrong code generated for 'str += (nullableChar ?: break)'
- [`KT-17966`](https://youtrack.jetbrains.com/issue/KT-17966) JS: Char literal inside of string template

### Libraries

- [`KT-17453`](https://youtrack.jetbrains.com/issue/KT-17453) Array iterators throw IndexOutOfBoundsException instead of NoSuchElementException
- [`KT-17635`](https://youtrack.jetbrains.com/issue/KT-17635) Document String#toIntOfNull may throw an exception
- [`KT-17686`](https://youtrack.jetbrains.com/issue/KT-17686) takeLast(n) incorrectly performs drop(n) for Lists without random access
- [`KT-17704`](https://youtrack.jetbrains.com/issue/KT-17704) Update JavaDoc for ReentrantReadWriteLock.write to put more stress on the fact that to upgrade to write lock, read lock is first released.
- [`KT-17853`](https://youtrack.jetbrains.com/issue/KT-17853) JS: Confusing parameter names in 'Math.atan2`
- [`KT-18092`](https://youtrack.jetbrains.com/issue/KT-18092) Issue using kotlin-reflect with proguard: missing annotations Mutable and ReadOnly
- [`KT-18210`](https://youtrack.jetbrains.com/issue/KT-18210) JS String::match(regex) should have nullable return type

### Reflection

- [`KT-17055`](https://youtrack.jetbrains.com/issue/KT-17055) NPE in hashCode and equals of kotlin.jvm.internal.FunctionReference (on local functions)
- [`KT-17594`](https://youtrack.jetbrains.com/issue/KT-17594) Cache the result of val Class<T>.kotlin: KClass<T>
- [`KT-18494`](https://youtrack.jetbrains.com/issue/KT-18494) KNPE from Kotlin reflection (sometimes) in UtilKt.toJavaClass

### Tools

- [`KT-16692`](https://youtrack.jetbrains.com/issue/KT-16692) No-Arg-Constructor plugin should generate code to initialize delegates

### Tools. CLI

- [`KT-17696`](https://youtrack.jetbrains.com/issue/KT-17696) Allow kotlinc to take friend modules as .jar files
- [`KT-17697`](https://youtrack.jetbrains.com/issue/KT-17697) Allow kotlinc to take .java files as arguments
- [`KT-9370`](https://youtrack.jetbrains.com/issue/KT-9370) not possible to pass an argument that starts with "-" to a script using kotlinc
- [`KT-17100`](https://youtrack.jetbrains.com/issue/KT-17100) "kotlin" launcher script: do not add current working directory to classpath if explicit "-classpath" is specified
- [`KT-17140`](https://youtrack.jetbrains.com/issue/KT-17140) Warning "classpath entry points to a file that is not a jar file" could just be disabled
- [`KT-17264`](https://youtrack.jetbrains.com/issue/KT-17264) Change the format of advanced CLI arguments ("-X...") to require value after "=", not a whitespace
- [`KT-18180`](https://youtrack.jetbrains.com/issue/KT-18180) Modules not exported by java.se are not readable when compiling against JRE 9

### Tools. Gradle

- [`KT-15151`](https://youtrack.jetbrains.com/issue/KT-15151) Kapt3: Support incremental compilation of Java stubs
- [`KT-16298`](https://youtrack.jetbrains.com/issue/KT-16298) Gradle: IOException "Parent file doesn't exist:/.../artifact-difference.tab.len" on non-incremental clean after incremental build
- [`KT-17681`](https://youtrack.jetbrains.com/issue/KT-17681) Support the new API of Android Gradle plugin (2.4.0+)
- [`KT-17936`](https://youtrack.jetbrains.com/issue/KT-17936) Circular dependency between gradle tasks dataBindingExportBuildInfoDebug and compileDebugKotlin
- [`KT-17960`](https://youtrack.jetbrains.com/issue/KT-17960) Improve test of memory leak with Gradle daemon
- [`KT-18047`](https://youtrack.jetbrains.com/issue/KT-18047) Gradle kotlin options should use unset value as default for languageVersion and apiVersion

### Tools. J2K

- [`KT-16754`](https://youtrack.jetbrains.com/issue/KT-16754) J2K: Apply quick-fixes from EDT thread only
- [`KT-16816`](https://youtrack.jetbrains.com/issue/KT-16816) Java To Kotlin bug: if + chained assignment doesn't include brackets
- [`KT-17230`](https://youtrack.jetbrains.com/issue/KT-17230) J2K Deadlock
- [`KT-17712`](https://youtrack.jetbrains.com/issue/KT-17712) Exception in J2K during InlineCodegen convertion: com.intellij.psi.impl.source.JavaDummyHolder cannot be cast to com.intellij.psi.PsiJavaFile

### Tools. JPS

- [`KT-16568`](https://youtrack.jetbrains.com/issue/KT-16568) modulesWhoseInternalsAreVisible in ModuleDependencies are not filled in for JS projects
- [`KT-17387`](https://youtrack.jetbrains.com/issue/KT-17387) When compiling in the IDE, progress tracker says "configuring the compilation environment" when it clearly isn't
- [`KT-17665`](https://youtrack.jetbrains.com/issue/KT-17665) JPS: Kotlin: The '-d' option with a directory destination is ignored because '-module' is specified
- [`KT-17801`](https://youtrack.jetbrains.com/issue/KT-17801) Unresolved supertypes from JRE on JDK 9 in JPS

### Tools. Maven

- [`KT-17093`](https://youtrack.jetbrains.com/issue/KT-17093) Import from maven: please provide a special tag for coroutine option
- [`KT-10028`](https://youtrack.jetbrains.com/issue/KT-10028) Support parallel builds in maven
- [`KT-15050`](https://youtrack.jetbrains.com/issue/KT-15050) Random build failures using maven 3 (multi-thread) + bamboo
- [`KT-15318`](https://youtrack.jetbrains.com/issue/KT-15318) Intermitent Kotlin compilation errors
- [`KT-16283`](https://youtrack.jetbrains.com/issue/KT-16283) Maven compiler plugin warns, "Source root doesn't exist"
- [`KT-16743`](https://youtrack.jetbrains.com/issue/KT-16743) Update configuration options in Kotlin Maven plugin
- [`KT-16762`](https://youtrack.jetbrains.com/issue/KT-16762) Maven: JS compiler option main is missing

### Tools. REPL

- [`KT-5822`](https://youtrack.jetbrains.com/issue/KT-5822) Exception on package directive in REPL
- [`KT-10060`](https://youtrack.jetbrains.com/issue/KT-10060) REPL: Cannot execute more than 255 lines
- [`KT-17365`](https://youtrack.jetbrains.com/issue/KT-17365) REPL crash when referencing a variable whose definition threw an exception

### Tools. kapt

- [`KT-17245`](https://youtrack.jetbrains.com/issue/KT-17245) Kapt: Javac compiler arguments can't be specified in Gradle
- [`KT-17418`](https://youtrack.jetbrains.com/issue/KT-17418) "The following options were not recognized by any processor: '[kapt.kotlin.generated]'" warning from Javac shouldn't be shown even if no processor supports the generated annotation
- [`KT-17456`](https://youtrack.jetbrains.com/issue/KT-17456) kapt3: NoClassDefFound com/sun/tools/javac/util/Context
- [`KT-17567`](https://youtrack.jetbrains.com/issue/KT-17567) Kapt (1.1.2-eap-77) generates invalid Java stub for internal class
- [`KT-17620`](https://youtrack.jetbrains.com/issue/KT-17620) Kapt3 IC: avoid running AP when API is not changed
- [`KT-17959`](https://youtrack.jetbrains.com/issue/KT-17959) Kapt3 doesn't preserve method parameter names for abstract methods
- [`KT-17999`](https://youtrack.jetbrains.com/issue/KT-17999) Cannot use KAPT3 1.1.2-4 in Android Studio java libs (null TypeCastException to WrappedVariantData<*> on Gradle Sync)

## Previous releases

This release also includes the fixes and improvements from the previous
[`1.1.2`](https://github.com/JetBrains/kotlin/blob/1.1.2/ChangeLog.md) release.