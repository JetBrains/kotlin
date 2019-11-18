# Content
The release contains the JVM version of Kotlin compiler and also one native system dependent version for every major platform (Linux, macOS, and Windows). 

# CHANGELOG

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

### Kotlin Native
Related [Kotlin Native changelog](https://github.com/JetBrains/kotlin-native/blob/master/CHANGELOG.md#v1360-oct-2019) can be found separately.