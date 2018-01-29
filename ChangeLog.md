# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.2.30 - Work In Progress

### Compiler

#### New Features

- [`KT-17336`](https://youtrack.jetbrains.com/issue/KT-17336) Introduce suspendCoroutineUninterceptedOrReturn coroutine intrinsic function
#### Performance Improvements

- [`KT-5177`](https://youtrack.jetbrains.com/issue/KT-5177) Optimize code generation for 'for' loop with withIndex()
- [`KT-16880`](https://youtrack.jetbrains.com/issue/KT-16880) Smarter detection of tail-suspending unit invocations
#### Fixes

- [`KT-10494`](https://youtrack.jetbrains.com/issue/KT-10494) IAE in CheckMethodAdapter.checkInternalName when declaring classes inside method with non-standard name
- [`KT-16079`](https://youtrack.jetbrains.com/issue/KT-16079) Internal error when using suspend operator plus
- [`KT-20705`](https://youtrack.jetbrains.com/issue/KT-20705) Tail suspend call optimization doesn't work in when block
- [`KT-20708`](https://youtrack.jetbrains.com/issue/KT-20708) Tail suspend call optiomization doesn't work in some branches
- [`KT-21165`](https://youtrack.jetbrains.com/issue/KT-21165) Exception from suspending function is not caught
- [`KT-21258`](https://youtrack.jetbrains.com/issue/KT-21258) Raw backing field value exposed via accessors?
- [`KT-21303`](https://youtrack.jetbrains.com/issue/KT-21303) Running on JDK-10-ea-31 leads to ArrayIndexOutOfBoundsException
- [`KT-21642`](https://youtrack.jetbrains.com/issue/KT-21642) Back-end (JVM) Internal error: Couldn't transform method node on using `open` keyword with `suspend` for a top-level function
- [`KT-21759`](https://youtrack.jetbrains.com/issue/KT-21759) Compiler crashes on two subsequent return statements in suspend function
- [`KT-22029`](https://youtrack.jetbrains.com/issue/KT-22029) Fold list to pair with destructuring assignment and inner when results in Exception
- [`KT-22345`](https://youtrack.jetbrains.com/issue/KT-22345) OOM in ReturnUnitMethodReplacer

### IDE

#### New Features

- [`KT-16833`](https://youtrack.jetbrains.com/issue/KT-16833) Allow mixing Java and Kotlin code in "Analyze Data Flow..."
#### Performance Improvements

- [`KT-21450`](https://youtrack.jetbrains.com/issue/KT-21450) Add caching for Module.languageVersionSettings
- [`KT-21517`](https://youtrack.jetbrains.com/issue/KT-21517) OOME during find usages
#### Fixes

- [`KT-7316`](https://youtrack.jetbrains.com/issue/KT-7316) Go to declaration in Kotlin JavaScript project navigates to JDK source in some cases
- [`KT-11467`](https://youtrack.jetbrains.com/issue/KT-11467) Editor: `var` property in primary constructor is shown not underscored, same as `val`
- [`KT-13509`](https://youtrack.jetbrains.com/issue/KT-13509) Don't show run line markers for top-level functions annotated with @Test
- [`KT-13971`](https://youtrack.jetbrains.com/issue/KT-13971) Kotlin Bytecode tool window: Decompile is available for incompilable code, CE at MemberCodegen.genFunctionOrProperty()
- [`KT-15000`](https://youtrack.jetbrains.com/issue/KT-15000) Do not spell check overridden declaration names
- [`KT-16333`](https://youtrack.jetbrains.com/issue/KT-16333) Cannot navigate to super declaration via shortcut
- [`KT-16976`](https://youtrack.jetbrains.com/issue/KT-16976) Introduce special SDK for Kotlin JS projects to avoid using JDK
- [`KT-19194`](https://youtrack.jetbrains.com/issue/KT-19194) Some Live Templates should probably be enabled also for "expressions" not only "statements"
- [`KT-20470`](https://youtrack.jetbrains.com/issue/KT-20470) IntelliJ indent guide/invisible brace matching hint tooltip doesn't show context
- [`KT-20915`](https://youtrack.jetbrains.com/issue/KT-20915) Add quickfix for ‘Implicit (unsafe) cast from dynamic type’ 
- [`KT-21688`](https://youtrack.jetbrains.com/issue/KT-21688) UIdentifier violates JvmDeclarationElement contract
- [`KT-21958`](https://youtrack.jetbrains.com/issue/KT-21958) Support "Alternative source available" for Kotlin files
- [`KT-21994`](https://youtrack.jetbrains.com/issue/KT-21994) Collapsed comments containing `*` get removed in the summary line.
- [`KT-22179`](https://youtrack.jetbrains.com/issue/KT-22179) For properties overridden in object literals, navigation to inherited properties is missing indication of a type they are overridden
- [`KT-22214`](https://youtrack.jetbrains.com/issue/KT-22214) Front-end Internal error: Failed to analyze declaration
- [`KT-22230`](https://youtrack.jetbrains.com/issue/KT-22230) Reformatting code to Kotlin style indents top-level typealiases with comments
- [`KT-22242`](https://youtrack.jetbrains.com/issue/KT-22242) Semantic highlighting uses different colors for the same 'it' variable and same color for different 'it's
- [`KT-22356`](https://youtrack.jetbrains.com/issue/KT-22356) Update status of inspection "Kotlin JVM compiler configured but no stdlib dependency" after pom file update, not on re-import
- [`KT-22374`](https://youtrack.jetbrains.com/issue/KT-22374) "Join lines" works incorrectly in case of line containing more than one string literal
- [`KT-22473`](https://youtrack.jetbrains.com/issue/KT-22473) Regression in IntelliJ Kotlin Plugin 1.2.20, settings.gradle.kts script template is wrong

### IDE. Debugger

- [`KT-20351`](https://youtrack.jetbrains.com/issue/KT-20351) Stepping over a line with two inline stdlib functions steps into the second function
- [`KT-21945`](https://youtrack.jetbrains.com/issue/KT-21945) Double stop on same line during step over if inline call is present

### IDE. Inspections and Intentions

#### New Features

- [`KT-22038`](https://youtrack.jetbrains.com/issue/KT-22038) Inspection to replace the usage of Java Collections methods on subtypes of MutableList with the methods from Kotlin stdlib
- [`KT-22171`](https://youtrack.jetbrains.com/issue/KT-22171) Add Intention for single character substring
- [`KT-22409`](https://youtrack.jetbrains.com/issue/KT-22409) Intention for changing property setter accessibility
#### Performance Improvements

- [`KT-21137`](https://youtrack.jetbrains.com/issue/KT-21137) Kotlin instantiates something expensive via reflection when highlighting Java file
#### Fixes

- [`KT-18007`](https://youtrack.jetbrains.com/issue/KT-18007) Inspection doesn't suggest Maven Plugin for kotlin-stdlib-jre8
- [`KT-18308`](https://youtrack.jetbrains.com/issue/KT-18308) 'Remove braces from else statement' intention breaks code
- [`KT-18912`](https://youtrack.jetbrains.com/issue/KT-18912) multiplatform project: Convert to enum class: header sealed class cannot convert nested objects to enum values
- [`KT-21600`](https://youtrack.jetbrains.com/issue/KT-21600) `suspend` modifier should go after `override` in overridden suspend functions
- [`KT-21881`](https://youtrack.jetbrains.com/issue/KT-21881) Replace "If" with safe access intention false positive
- [`KT-22054`](https://youtrack.jetbrains.com/issue/KT-22054) Replace '!=' with 'contentEquals' should be replace '==' with 'contentEquals'
- [`KT-22159`](https://youtrack.jetbrains.com/issue/KT-22159) "Replace return with 'if' expression" should not place return before expressions of type Nothing
- [`KT-22221`](https://youtrack.jetbrains.com/issue/KT-22221) QuickFix to remove unused constructor parameters shouldn't delete parenthesis
- [`KT-22335`](https://youtrack.jetbrains.com/issue/KT-22335) IOE from KotlinUnusedImportInspection.scheduleOptimizeImportsOnTheFly
- [`KT-22339`](https://youtrack.jetbrains.com/issue/KT-22339) Remove setter parameter type: error while creating problem descriptor

### IDE. Refactorings

- [`KT-17047`](https://youtrack.jetbrains.com/issue/KT-17047) Refactorings for related standard "scoping functions" conversion: 'let' <-> 'run', 'apply' <-> 'also'
- [`KT-22200`](https://youtrack.jetbrains.com/issue/KT-22200) Overriden function generated from completion is missing suspend modifier
- [`KT-22359`](https://youtrack.jetbrains.com/issue/KT-22359) Refactor / Rename file: Throwable at RenameProcessor.performRefactoring()
- [`KT-22461`](https://youtrack.jetbrains.com/issue/KT-22461) Rename doesn't work on private top-level members of multi-file parts

### JavaScript

- [`KT-20735`](https://youtrack.jetbrains.com/issue/KT-20735) JS: kotlin.test-js integration tests terminate build on failure

### Libraries

- [`KT-16661`](https://youtrack.jetbrains.com/issue/KT-16661) Performance overhead in string splitting in Kotlin versus Java?
- [`KT-22042`](https://youtrack.jetbrains.com/issue/KT-22042) Suboptimal `Strings#findAnyOf`

### Tools

- [`KT-22196`](https://youtrack.jetbrains.com/issue/KT-22196) kotlin-compiler-embeddable bundles outdated kotlinx.coroutines since 1.1.60

### Tools. CLI

- [`KT-19051`](https://youtrack.jetbrains.com/issue/KT-19051) Suppress Java 9 illegal access warnings

### Tools. Gradle

- [`KT-18462`](https://youtrack.jetbrains.com/issue/KT-18462) Add 'org.jetbrains.kotlin.platform.android' plugin.

### Tools. J2K

- [`KT-21635`](https://youtrack.jetbrains.com/issue/KT-21635) J2K: create "inspection based post-processing"�

## Previous releases

This release also includes the fixes and improvements from the previous
[`1.2.21`](https://github.com/JetBrains/kotlin/blob/1.2.20/ChangeLog.md) release.
