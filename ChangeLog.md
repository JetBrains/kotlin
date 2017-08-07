# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.2-M2

### Android

- [`KT-16934`](https://youtrack.jetbrains.com/issue/KT-16934) Android Extensions fails to compile when importing synthetic properties for layouts in other modules

### Compiler

- [`KT-15825`](https://youtrack.jetbrains.com/issue/KT-15825) Switch warning to error for java-default method calls within 1.6 target
- [`KT-18702`](https://youtrack.jetbrains.com/issue/KT-18702) Do not use non-existing class for suspend markers
- [`KT-18845`](https://youtrack.jetbrains.com/issue/KT-18845) Exception on building gradle project with collection literals
- [`KT-19251`](https://youtrack.jetbrains.com/issue/KT-19251) Generate SAM wrappers only if they are required for a given argument
- [`KT-19441`](https://youtrack.jetbrains.com/issue/KT-19441) Fix collection literals resolve in gradle-based projects

### IDE. Inspections and Intentions

- [`KT-18160`](https://youtrack.jetbrains.com/issue/KT-18160) Circular autofix actions between redundant modality and non-final variable with allopen plugin
- [`KT-18194`](https://youtrack.jetbrains.com/issue/KT-18194) "Protected in final" inspection works incorrectly with all-open
- [`KT-18195`](https://youtrack.jetbrains.com/issue/KT-18195) "Redundant modality" is not reported with all-open
- [`KT-18197`](https://youtrack.jetbrains.com/issue/KT-18197) Redundant "make open" for abstract class member with all-open

### Standard Library

- [`KEEP-11`](https://github.com/Kotlin/KEEP/blob/master/proposals/stdlib/window-sliding.md)
    - `windowed` function: make default step equals to 1, add `partialWindows` parameter
    - rename `pairwise` function to `zipWithNext` 
- [`KT-4900`](https://youtrack.jetbrains.com/issue/KT-4900) Support math operations in stdlib
- [`KT-18264`](https://youtrack.jetbrains.com/issue/KT-18264) Provide Double and Float bit conversion functions as extensions

### Tools. kapt

- [`KT-18799`](https://youtrack.jetbrains.com/issue/KT-18799) Kapt3, IC: Kapt does not generate annotation value for constant values in documented types

## Previous releases

This release also includes the fixes and improvements from 
[`1.2-M1`](https://github.com/JetBrains/kotlin/blob/1.2-M1/ChangeLog.md) and [`1.1.4-EAP-54`](https://github.com/JetBrains/kotlin/blob/1.1.4/ChangeLog.md#114-eap-54) releases.