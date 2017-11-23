# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.2-RC2

### Compiler

- [`KT-20844`](https://youtrack.jetbrains.com/issue/KT-20844) VerifyError on Android after upgrading to 1.2.0-beta-88
- [`KT-20895`](https://youtrack.jetbrains.com/issue/KT-20895) NPE in Kotlin 1.2-beta88 PseudocodeVariablesData.kt:337 
- [`KT-21377`](https://youtrack.jetbrains.com/issue/KT-21377) Create fallback flag for "Illegal smart cast is allowed after assignment in try block"

### IDE

- [`KT-18719`](https://youtrack.jetbrains.com/issue/KT-18719) Configure Kotlin in Gradle project to 1.2-Mx: add repository mavenCentral() to buildscript
- [`KT-20782`](https://youtrack.jetbrains.com/issue/KT-20782) Exception when working with routing in kapt (non-atomic trees update)
- [`KT-20966`](https://youtrack.jetbrains.com/issue/KT-20966) ISE: Facade class not found from Kotlin test files
- [`KT-20967`](https://youtrack.jetbrains.com/issue/KT-20967) Kotlin plugin upgrade breaks Gradle refresh
- [`KT-20990`](https://youtrack.jetbrains.com/issue/KT-20990) String literal in string template causes ISE
- [`KT-21028`](https://youtrack.jetbrains.com/issue/KT-21028) Add kotlin-stdlib-jre7/8 instead of kotlin-stdlib-jdk7/8 for Kotlin versions below 1.2
- [`KT-21383`](https://youtrack.jetbrains.com/issue/KT-21383) `Unsupported method: Library.getProject()` when importing Anko project
- Downgrade "use expression body" inspection to INFORMATION default level

### IDE. Debugger

- [`KT-20962`](https://youtrack.jetbrains.com/issue/KT-20962) NullPointerException because of nullable location in debugger

### IDE. Inspections and Intentions

- [`KT-20803`](https://youtrack.jetbrains.com/issue/KT-20803) Create actual declaration in the same source root as expect declaration

### IDE. Refactorings

- [`KT-20979`](https://youtrack.jetbrains.com/issue/KT-20979) Move class refactoring doesn't work anymore

### Libraries

- Remove deprecated `pairwise` function

### Tools. Gradle

- [`KT-21395`](https://youtrack.jetbrains.com/issue/KT-21395) “Unable to load class 'kotlin.collections.CollectionsKt'” on creating gradle project in IDEA 2016.3.7

### Tools. kapt

- Add `kotlin-annotation-processing-embeddable` artifact (compatible with `kotlin-compiler-embeddable`)
- Return `kotlin-annotation-processing` artifact back (compatible with CLI Kotlin compiler)

## Previous releases

This release also includes the fixes and improvements from the previous
[`1.2-RC1`](https://github.com/JetBrains/kotlin/releases/tag/v1.2-rc1) release.