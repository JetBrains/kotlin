# CHANGELOG

## 1.2.30 - Work In Progress

### Android

- [`KT-19300`](https://youtrack.jetbrains.com/issue/KT-19300) [AS3.0] Android extensions, Parcelable: editor shows warning about incomplete implementation on a class with Parcelize annotation
- [`KT-22168`](https://youtrack.jetbrains.com/issue/KT-22168) "Kotlin Android | Illegal Android Identifier" inspection reports non-instrumentation unit tests
- [`KT-22700`](https://youtrack.jetbrains.com/issue/KT-22700) Android Extensions bind views with dot in ID

### Compiler

#### New Features

- [`KT-17336`](https://youtrack.jetbrains.com/issue/KT-17336) Introduce suspendCoroutineUninterceptedOrReturn coroutine intrinsic function
- [`KT-22766`](https://youtrack.jetbrains.com/issue/KT-22766) Imitate "suspend" modifier in 1.2.x by stdlib function
#### Performance Improvements

- [`KT-5177`](https://youtrack.jetbrains.com/issue/KT-5177) Optimize code generation for 'for' loop with withIndex()
- [`KT-16880`](https://youtrack.jetbrains.com/issue/KT-16880) Smarter detection of tail-suspending unit invocations
#### Fixes

- [`KT-10494`](https://youtrack.jetbrains.com/issue/KT-10494) IAE in CheckMethodAdapter.checkInternalName when declaring classes inside method with non-standard name
- [`KT-16079`](https://youtrack.jetbrains.com/issue/KT-16079) Internal error when using suspend operator plus
- [`KT-17091`](https://youtrack.jetbrains.com/issue/KT-17091) Converting to SAM Java type appends non-deterministic hash to class name
- [`KT-18522`](https://youtrack.jetbrains.com/issue/KT-18522) Internal compiler error with IndexOutOfBoundsException, "Exception while analyzing expression"
- [`KT-18578`](https://youtrack.jetbrains.com/issue/KT-18578) Compilation failure with @JsonInclude and default interface method
- [`KT-19786`](https://youtrack.jetbrains.com/issue/KT-19786) Kotlin — unable to override a Java function with @Nullable vararg argument
- [`KT-20466`](https://youtrack.jetbrains.com/issue/KT-20466) JSR305 false positive for elvis operator
- [`KT-20705`](https://youtrack.jetbrains.com/issue/KT-20705) Tail suspend call optimization doesn't work in when block
- [`KT-20708`](https://youtrack.jetbrains.com/issue/KT-20708) Tail suspend call optiomization doesn't work in some branches
- [`KT-20855`](https://youtrack.jetbrains.com/issue/KT-20855) Unnecessary safe-call reported on nullable type
- [`KT-21165`](https://youtrack.jetbrains.com/issue/KT-21165) Exception from suspending function is not caught
- [`KT-21238`](https://youtrack.jetbrains.com/issue/KT-21238) Nonsensical warning "Expected type does not accept nulls in Java, but the value may be null in Kotlin"
- [`KT-21258`](https://youtrack.jetbrains.com/issue/KT-21258) Raw backing field value exposed via accessors?
- [`KT-21303`](https://youtrack.jetbrains.com/issue/KT-21303) Running on JDK-10-ea-31 leads to ArrayIndexOutOfBoundsException
- [`KT-21642`](https://youtrack.jetbrains.com/issue/KT-21642) Back-end (JVM) Internal error: Couldn't transform method node on using `open` keyword with `suspend` for a top-level function
- [`KT-21759`](https://youtrack.jetbrains.com/issue/KT-21759) Compiler crashes on two subsequent return statements in suspend function
- [`KT-22029`](https://youtrack.jetbrains.com/issue/KT-22029) Fold list to pair with destructuring assignment and inner when results in Exception
- [`KT-22345`](https://youtrack.jetbrains.com/issue/KT-22345) OOM in ReturnUnitMethodReplacer
- [`KT-22410`](https://youtrack.jetbrains.com/issue/KT-22410) invalid compiler optimization for nullable cast to reified type
- [`KT-22577`](https://youtrack.jetbrains.com/issue/KT-22577) Compiler crashes when coroutineContext is used inside of inlined lambda

### IDE

#### New Features

- [`KT-8352`](https://youtrack.jetbrains.com/issue/KT-8352) Pasting Kotlin code into package could create .kt file
- [`KT-16710`](https://youtrack.jetbrains.com/issue/KT-16710) Run configuration to run main() as a Node CLI app
- [`KT-16833`](https://youtrack.jetbrains.com/issue/KT-16833) Allow mixing Java and Kotlin code in "Analyze Data Flow..."
- [`KT-21531`](https://youtrack.jetbrains.com/issue/KT-21531) JS: add support for running specific test from the gutter icon with Jest testing framework
#### Performance Improvements

- [`KT-21450`](https://youtrack.jetbrains.com/issue/KT-21450) Add caching for Module.languageVersionSettings
- [`KT-21517`](https://youtrack.jetbrains.com/issue/KT-21517) OOME during find usages
#### Fixes

- [`KT-7316`](https://youtrack.jetbrains.com/issue/KT-7316) Go to declaration in Kotlin JavaScript project navigates to JDK source in some cases
- [`KT-8563`](https://youtrack.jetbrains.com/issue/KT-8563) Refactor / Rename inserts line breaks without reason
- [`KT-11467`](https://youtrack.jetbrains.com/issue/KT-11467) Editor: `var` property in primary constructor is shown not underscored, same as `val`
- [`KT-13509`](https://youtrack.jetbrains.com/issue/KT-13509) Don't show run line markers for top-level functions annotated with @Test
- [`KT-13971`](https://youtrack.jetbrains.com/issue/KT-13971) Kotlin Bytecode tool window: Decompile is available for incompilable code, CE at MemberCodegen.genFunctionOrProperty()
- [`KT-15000`](https://youtrack.jetbrains.com/issue/KT-15000) Do not spell check overridden declaration names
- [`KT-15331`](https://youtrack.jetbrains.com/issue/KT-15331) "Kotlin not configured" notification always shown for common module in multiplatform project
- [`KT-16333`](https://youtrack.jetbrains.com/issue/KT-16333) Cannot navigate to super declaration via shortcut
- [`KT-16976`](https://youtrack.jetbrains.com/issue/KT-16976) Introduce special SDK for Kotlin JS projects to avoid using JDK
- [`KT-18445`](https://youtrack.jetbrains.com/issue/KT-18445) multiplatform project: provide more comfortable way to process cases when there are missed method implemenation in the implementation class
- [`KT-19194`](https://youtrack.jetbrains.com/issue/KT-19194) Some Live Templates should probably be enabled also for "expressions" not only "statements"
- [`KT-20281`](https://youtrack.jetbrains.com/issue/KT-20281) multiplatform:Unresolved service JavaDescriptorResolver on a file with several header declarations and gutters not shown
- [`KT-20470`](https://youtrack.jetbrains.com/issue/KT-20470) IntelliJ indent guide/invisible brace matching hint tooltip doesn't show context
- [`KT-20522`](https://youtrack.jetbrains.com/issue/KT-20522) Add "Build" action in "Before launch" block when create new JS run configuration (for test)
- [`KT-20915`](https://youtrack.jetbrains.com/issue/KT-20915) Add quickfix for ‘Implicit (unsafe) cast from dynamic type’
- [`KT-20971`](https://youtrack.jetbrains.com/issue/KT-20971) Cannot navigate to sources of compiled common dependency
- [`KT-21115`](https://youtrack.jetbrains.com/issue/KT-21115) Incomplete actual class should still have navigation icon to expect class
- [`KT-21688`](https://youtrack.jetbrains.com/issue/KT-21688) UIdentifier violates JvmDeclarationElement contract
- [`KT-21874`](https://youtrack.jetbrains.com/issue/KT-21874) Unexpected IDE error "Unknown type [typealias ...]"
- [`KT-21958`](https://youtrack.jetbrains.com/issue/KT-21958) Support "Alternative source available" for Kotlin files
- [`KT-21994`](https://youtrack.jetbrains.com/issue/KT-21994) Collapsed comments containing `*` get removed in the summary line.
- [`KT-22179`](https://youtrack.jetbrains.com/issue/KT-22179) For properties overridden in object literals, navigation to inherited properties is missing indication of a type they are overridden
- [`KT-22214`](https://youtrack.jetbrains.com/issue/KT-22214) Front-end Internal error: Failed to analyze declaration
- [`KT-22230`](https://youtrack.jetbrains.com/issue/KT-22230) Reformatting code to Kotlin style indents top-level typealiases with comments
- [`KT-22242`](https://youtrack.jetbrains.com/issue/KT-22242) Semantic highlighting uses different colors for the same 'it' variable and same color for different 'it's
- [`KT-22301`](https://youtrack.jetbrains.com/issue/KT-22301) Don't require space after label for lambda
- [`KT-22346`](https://youtrack.jetbrains.com/issue/KT-22346) Incorrect indentation for chained context extension functions (lambdas) when using Kotlin style guide
- [`KT-22356`](https://youtrack.jetbrains.com/issue/KT-22356) Update status of inspection "Kotlin JVM compiler configured but no stdlib dependency" after pom file update, not on re-import
- [`KT-22360`](https://youtrack.jetbrains.com/issue/KT-22360) MPP: with "Create separate module per source set" = No `expectedBy` dependency is imported not transitively
- [`KT-22374`](https://youtrack.jetbrains.com/issue/KT-22374) "Join lines" works incorrectly in case of line containing more than one string literal
- [`KT-22473`](https://youtrack.jetbrains.com/issue/KT-22473) Regression in IntelliJ Kotlin Plugin 1.2.20, settings.gradle.kts script template is wrong
- [`KT-22508`](https://youtrack.jetbrains.com/issue/KT-22508) Auto-formatting should insert an indentation for default parameter values
- [`KT-22557`](https://youtrack.jetbrains.com/issue/KT-22557) Dead 'Apply' button, when setting code style
- [`KT-22565`](https://youtrack.jetbrains.com/issue/KT-22565) Cant do `PsiAnchor.create` on annotation in annotation
- [`KT-22570`](https://youtrack.jetbrains.com/issue/KT-22570) Can't add import in "Packages to Use Import with '*'" section on "Import" tab in Code Style -> Kotlin
- [`KT-22641`](https://youtrack.jetbrains.com/issue/KT-22641) Auto-formatting adds extra indent to a closing square bracket on a separate line
- [`KT-22734`](https://youtrack.jetbrains.com/issue/KT-22734) LinkageError: "loader constraint violation: when resolving method PsiTreeUtilKt.parentOfType()" at KotlinConverter.convertPsiElement$uast_kotlin()

### IDE. Debugger

- [`KT-20351`](https://youtrack.jetbrains.com/issue/KT-20351) Stepping over a line with two inline stdlib functions steps into the second function
- [`KT-21312`](https://youtrack.jetbrains.com/issue/KT-21312) Confusing Kotlin (JavaScript) run configuration
- [`KT-21945`](https://youtrack.jetbrains.com/issue/KT-21945) Double stop on same line during step over if inline call is present

### IDE. Inspections and Intentions

#### New Features

- [`KT-18124`](https://youtrack.jetbrains.com/issue/KT-18124) Inspection to get rid of unnecessary ticks in references
- [`KT-22038`](https://youtrack.jetbrains.com/issue/KT-22038) Inspection to replace the usage of Java Collections methods on subtypes of MutableList with the methods from Kotlin stdlib
- [`KT-22152`](https://youtrack.jetbrains.com/issue/KT-22152) "Create Class" quickfix should support creating the class in a new file and selecting the package for that file
- [`KT-22171`](https://youtrack.jetbrains.com/issue/KT-22171) Add Intention for single character substring
- [`KT-22303`](https://youtrack.jetbrains.com/issue/KT-22303) Inspection to detect `Type!.inlineWithNotNullReceiver()` calls
- [`KT-22409`](https://youtrack.jetbrains.com/issue/KT-22409) Intention for changing property setter accessibility
#### Performance Improvements

- [`KT-21137`](https://youtrack.jetbrains.com/issue/KT-21137) Kotlin instantiates something expensive via reflection when highlighting Java file
#### Fixes

- [`KT-15176`](https://youtrack.jetbrains.com/issue/KT-15176) Remove "Create type alias" intention when called on java class
- [`KT-18007`](https://youtrack.jetbrains.com/issue/KT-18007) Inspection doesn't suggest Maven Plugin for kotlin-stdlib-jre8
- [`KT-18308`](https://youtrack.jetbrains.com/issue/KT-18308) 'Remove braces from else statement' intention breaks code
- [`KT-18912`](https://youtrack.jetbrains.com/issue/KT-18912) multiplatform project: Convert to enum class: header sealed class cannot convert nested objects to enum values
- [`KT-21114`](https://youtrack.jetbrains.com/issue/KT-21114) IOE: create actual members for expected with companion
- [`KT-21600`](https://youtrack.jetbrains.com/issue/KT-21600) `suspend` modifier should go after `override` in overridden suspend functions
- [`KT-21881`](https://youtrack.jetbrains.com/issue/KT-21881) Replace "If" with safe access intention false positive
- [`KT-22054`](https://youtrack.jetbrains.com/issue/KT-22054) Replace '!=' with 'contentEquals' should be replace '==' with 'contentEquals'
- [`KT-22097`](https://youtrack.jetbrains.com/issue/KT-22097) Redundant Unit inspection false positive for single expression function
- [`KT-22159`](https://youtrack.jetbrains.com/issue/KT-22159) "Replace return with 'if' expression" should not place return before expressions of type Nothing
- [`KT-22167`](https://youtrack.jetbrains.com/issue/KT-22167) "Add annotation target" quick fix does nothing and disappears from menu
- [`KT-22221`](https://youtrack.jetbrains.com/issue/KT-22221) QuickFix to remove unused constructor parameters shouldn't delete parenthesis
- [`KT-22335`](https://youtrack.jetbrains.com/issue/KT-22335) IOE from KotlinUnusedImportInspection.scheduleOptimizeImportsOnTheFly
- [`KT-22339`](https://youtrack.jetbrains.com/issue/KT-22339) Remove setter parameter type: error while creating problem descriptor
- [`KT-22364`](https://youtrack.jetbrains.com/issue/KT-22364) Redundant setter is not reported for overridden fields
- [`KT-22484`](https://youtrack.jetbrains.com/issue/KT-22484) The warning highlight for redundant `!is`check for object types isn't extended to the full operator
- [`KT-22538`](https://youtrack.jetbrains.com/issue/KT-22538) "Redundant type checks for object" inspection application breaks smart cast for an object's field or function

### IDE. Refactorings

#### New Features

- [`KT-17047`](https://youtrack.jetbrains.com/issue/KT-17047) Refactorings for related standard "scoping functions" conversion: 'let' <-> 'run', 'apply' <-> 'also'
#### Fixes

- [`KT-12365`](https://youtrack.jetbrains.com/issue/KT-12365) Renaming `invoke` function should remove `operator` modifier and insert function call for implicit usages
- [`KT-17977`](https://youtrack.jetbrains.com/issue/KT-17977) Move class to upper level creates file with wrong file name
- [`KT-21719`](https://youtrack.jetbrains.com/issue/KT-21719) Actual typealias not renamed on expected declaration rename
- [`KT-22200`](https://youtrack.jetbrains.com/issue/KT-22200) Overriden function generated from completion is missing suspend modifier
- [`KT-22359`](https://youtrack.jetbrains.com/issue/KT-22359) Refactor / Rename file: Throwable at RenameProcessor.performRefactoring()
- [`KT-22461`](https://youtrack.jetbrains.com/issue/KT-22461) Rename doesn't work on private top-level members of multi-file parts
- [`KT-22476`](https://youtrack.jetbrains.com/issue/KT-22476) Rename `it` parameter fails after replacing for-each with mapNotNull
- [`KT-22564`](https://youtrack.jetbrains.com/issue/KT-22564) Rename doesn't warn for conflicts
- [`KT-22705`](https://youtrack.jetbrains.com/issue/KT-22705) Refactor/Rename: rename of `invoke` function with lambda parameter to `get` breaks an implicit call
- [`KT-22708`](https://youtrack.jetbrains.com/issue/KT-22708) Refactor/Rename function using some stdlib name leads to incompilable code

### JavaScript

- [`KT-20735`](https://youtrack.jetbrains.com/issue/KT-20735) JS: kotlin.test-js integration tests terminate build on failure
- [`KT-22638`](https://youtrack.jetbrains.com/issue/KT-22638) Function reference not working in js from extension

### Libraries

- [`KT-22620`](https://youtrack.jetbrains.com/issue/KT-22620) Add support for TestNG in kotlin.test
- [`KT-16661`](https://youtrack.jetbrains.com/issue/KT-16661) Performance overhead in string splitting in Kotlin versus Java?
- [`KT-22042`](https://youtrack.jetbrains.com/issue/KT-22042) Suboptimal `Strings#findAnyOf`
- [`KT-21154`](https://youtrack.jetbrains.com/issue/KT-21154) kotlin-test-junit doesn't provide JUnitAsserter when test body is run in another thread

### Tools

- [`KT-22196`](https://youtrack.jetbrains.com/issue/KT-22196) kotlin-compiler-embeddable bundles outdated kotlinx.coroutines since 1.1.60
- [`KT-22549`](https://youtrack.jetbrains.com/issue/KT-22549) Service is dying during compilation

### Tools. CLI

- [`KT-19051`](https://youtrack.jetbrains.com/issue/KT-19051) Suppress Java 9 illegal access warnings

### Tools. Gradle

- [`KT-18462`](https://youtrack.jetbrains.com/issue/KT-18462) Add 'org.jetbrains.kotlin.platform.android' plugin.
- [`KT-18821`](https://youtrack.jetbrains.com/issue/KT-18821) Gradle plugin should not resolve dependencies at configuration time

### Tools. Incremental Compile

- [`KT-22192`](https://youtrack.jetbrains.com/issue/KT-22192) Make precise java classes tracking in Gradle enabled by default

### Tools. J2K

- [`KT-21635`](https://youtrack.jetbrains.com/issue/KT-21635) J2K: create "inspection based post-processing"

### Tools. REPL

- [`KT-12037`](https://youtrack.jetbrains.com/issue/KT-12037) REPL crashes when trying to :load with incorrect filename

### Tools. kapt

- [`KT-22350`](https://youtrack.jetbrains.com/issue/KT-22350) kdoc comment preceding enum method causes compilation failure
- [`KT-22386`](https://youtrack.jetbrains.com/issue/KT-22386) kapt3 fails when project has class named System
- [`KT-22468`](https://youtrack.jetbrains.com/issue/KT-22468) Kapt fails to convert array type to anonymous array element type
- [`KT-22469`](https://youtrack.jetbrains.com/issue/KT-22469) Kapt 1.2.20+ may fail to process classes with KDoc
- [`KT-22493`](https://youtrack.jetbrains.com/issue/KT-22493) Kapt: NoSuchElementException in KotlinCliJavaFileManagerImpl if class first character is dollar sign
- [`KT-22582`](https://youtrack.jetbrains.com/issue/KT-22582) Kapt: Enums inside enum values should be forbidden
- [`KT-22711`](https://youtrack.jetbrains.com/issue/KT-22711) Deprecate original kapt (aka kapt1)


## Previous releases

This release also includes the fixes and improvements from the previous
[`1.2.21`](https://github.com/JetBrains/kotlin/blob/1.2.20/ChangeLog.md) release.
