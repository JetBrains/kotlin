# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.2-M1

### Language changes

- Array literals, which can be used in _annotation_ arguments

### Compiler

- [`KT-6884`](https://youtrack.jetbrains.com/issue/KT-6884) 
  [`KT-17910`](https://youtrack.jetbrains.com/issue/KT-17910) 
  Support default values for functional parameters in inline functions
- [`KT-15894`](https://youtrack.jetbrains.com/issue/KT-15894) Change the way how singleton objects are initialized in order not to contradict JVM spec 
- [`KT-17929`](https://youtrack.jetbrains.com/issue/KT-17929) Illegal smart cast was allowed after assignment in try block

### Standard Library

- [`KT-8823`](https://youtrack.jetbrains.com/issue/KT-8823) `MutableList.fill` extension
- [`KT-9010`](https://youtrack.jetbrains.com/issue/KT-9010) `MutableList.shuffle` and `List.shuffled` extensions
- [`KEEP-11`](https://github.com/Kotlin/KEEP/blob/master/proposals/stdlib/window-sliding.md)
  [`KT-9151`](https://youtrack.jetbrains.com/issue/KT-9151) 
  [`KT-10021`](https://youtrack.jetbrains.com/issue/KT-10021)
  [`KT-11026`](https://youtrack.jetbrains.com/issue/KT-11026)
  `chunked` and `windowed`: extension functions to support 
  partitioning collections into blocks of the given size and
  taking a window of the given size and moving it along the collection with the given step.
  
  `pairwise` extension to get all subsequent pairs in collection
  
- [`KEEP-49`](https://github.com/Kotlin/KEEP/blob/master/proposals/stdlib/bignumber-operations.md) Additional operations and conversion extensions for `BigInteger` and `BigDecimal`
- [`KT-16447`](https://youtrack.jetbrains.com/issue/KT-16447) Make `kotlin.text.Regex` class serializable

### IDE

- [`KT-17164`](https://youtrack.jetbrains.com/issue/KT-17164) Intention to convert `*arrayOf()` functions to array literals in annotations

### JS

- Typed arrays are turned on by default


## Previous releases

This release also includes the fixes and improvements from 
[`1.1.3`](https://github.com/JetBrains/kotlin/blob/1.1.3/ChangeLog.md) release.

