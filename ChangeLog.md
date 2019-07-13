# CHANGELOG

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

### IDE. Completion

- [`KT-9792`](https://youtrack.jetbrains.com/issue/KT-9792) Don't propose the same name for arguments of lambda on completion of function call with lambda template
- [`KT-29572`](https://youtrack.jetbrains.com/issue/KT-29572) Smart completing anonymous object uses incorrect code style

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

### IDE. Gradle

- [`KT-19693`](https://youtrack.jetbrains.com/issue/KT-19693) Import package prefix from Gradle
- [`KT-30667`](https://youtrack.jetbrains.com/issue/KT-30667) Dependencies of a module on a multiplatform one with a JVM target and `withJava()` configured, are incorrectly resolved in IDE
- [`KT-32300`](https://youtrack.jetbrains.com/issue/KT-32300) Add possibility to distinguish kotlin source root from java source root

### IDE. Gradle. Script

- [`KT-31779`](https://youtrack.jetbrains.com/issue/KT-31779) "Highlighting in scripts is not available"

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

### IDE. KDoc

- [`KT-30985`](https://youtrack.jetbrains.com/issue/KT-30985) Missing line break in quick doc for enum constant

### IDE. Navigation

- [`KT-10215`](https://youtrack.jetbrains.com/issue/KT-10215) Kotlin classes are listed after Java classes in the navigation bar

### IDE. Refactorings

- [`KT-29720`](https://youtrack.jetbrains.com/issue/KT-29720) Refactor / Move does not update package statement with implicit prefix
- [`KT-30762`](https://youtrack.jetbrains.com/issue/KT-30762) Inline method produces invalid code for suspend functions with receiver

### IDE. Scratch

- [`KT-23604`](https://youtrack.jetbrains.com/issue/KT-23604) Scratch: end of line is wrongly indented with the end of scratch line output 
- [`KT-27963`](https://youtrack.jetbrains.com/issue/KT-27963) Make REPL mode in Scratch files incremental
- [`KT-29534`](https://youtrack.jetbrains.com/issue/KT-29534) Line output jumps to the next line together with cursor

### IDE. Script

- [`KT-25187`](https://youtrack.jetbrains.com/issue/KT-25187) Kotlin script in src: warning: classpath entry points to a non-existent location on JDK 9+
- [`KT-31152`](https://youtrack.jetbrains.com/issue/KT-31152) Errors in IDE when different Java Sdk are set as Project SDK and as Gradle JVM
- [`KT-31521`](https://youtrack.jetbrains.com/issue/KT-31521) CNFE „org.jetbrains.kotlin.idea.caches.project.ScriptBinariesScopeCache“ on creating new Gradle based project
- [`KT-31826`](https://youtrack.jetbrains.com/issue/KT-31826) Gradle clean task causes IDEA to lose kotlin scripting configuration
- [`KT-31837`](https://youtrack.jetbrains.com/issue/KT-31837) TargetPlatform for scripts should depends on scriptDefinition.additionalArguments

### IDE. Tests Support

- [`KT-30814`](https://youtrack.jetbrains.com/issue/KT-30814) MPP, 191 platform: with Gradle test runner run configuration for platform test is created without tasks

### JS. Tools

- [`KT-31527`](https://youtrack.jetbrains.com/issue/KT-31527) Keep generating empty `jsTest` task
- [`KT-31565`](https://youtrack.jetbrains.com/issue/KT-31565) Gradle/JS: `npmResolve` is never UP-TO-DATE
- [`KT-32326`](https://youtrack.jetbrains.com/issue/KT-32326) Gradle, test runner: support postponing test running error reporting at the end of the build
- [`KT-32393`](https://youtrack.jetbrains.com/issue/KT-32393) Gradle, JS: Resolve projects lazily

### Libraries

- [`KT-29372`](https://youtrack.jetbrains.com/issue/KT-29372) measureTime that returns both the result of block and elapsed time
- [`KT-32083`](https://youtrack.jetbrains.com/issue/KT-32083) Incorrect ReplaceWith annotation on kotlin.js.pow

### Reflection

- [`KT-22923`](https://youtrack.jetbrains.com/issue/KT-22923) Reflection getMemberProperties fails: kotlin.reflect.jvm.internal.KotlinReflectionInternalError
- [`KT-31318`](https://youtrack.jetbrains.com/issue/KT-31318) "KotlinReflectionInternalError: Method is not supported" on accessing array class annotation parameter

### Tools. Daemon

- [`KT-31550`](https://youtrack.jetbrains.com/issue/KT-31550) NSME org.jetbrains.kotlin.com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem.clearHandlersCache()V on compileKotlin task with plugin from master

### Tools. Gradle

#### New Features

- [`KT-26655`](https://youtrack.jetbrains.com/issue/KT-26655) Precise metadata publishing and consumption for new MPP
- [`KT-31018`](https://youtrack.jetbrains.com/issue/KT-31018) Gradle, JS: yarn
- [`KT-31703`](https://youtrack.jetbrains.com/issue/KT-31703) Gradle, JS: automatically download d.ts and generate kotlin/js external declarations using dukat
- [`KT-31890`](https://youtrack.jetbrains.com/issue/KT-31890) Gradle, JS, webpack: provide property with full bundle file path
- [`KT-32015`](https://youtrack.jetbrains.com/issue/KT-32015) Gradle, JS: resolve configuration only while executing tasks of specific projects
- [`KT-32136`](https://youtrack.jetbrains.com/issue/KT-32136) Gradle, test runner: handle case when test runtime exits abnormally

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

### Tools. JPS

- [`KT-27181`](https://youtrack.jetbrains.com/issue/KT-27181) Compiler arguments are listed twice on JPS build of Gradle-based project

### Tools. REPL

- [`KT-15125`](https://youtrack.jetbrains.com/issue/KT-15125) Support JSR 223 bindings directly via script variables
- [`KT-32085`](https://youtrack.jetbrains.com/issue/KT-32085) Kotlinc REPL: "java.lang.NoClassDefFoundError: org/jline/reader/LineReaderBuilder"

### Tools. kapt

- [`KT-30578`](https://youtrack.jetbrains.com/issue/KT-30578) `build/generated/source/kaptKotlin` is added as source directory to `main` instead of `jvmMain` when jvm { withJava() } is configured in a multiplatform project

## Previous releases
This release also includes the fixes and improvements from the [previous releases](https://github.com/JetBrains/kotlin/releases/tag/v1.3.41).