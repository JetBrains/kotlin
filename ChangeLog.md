# CHANGELOG

## 1.2.10

### Compiler

- [`KT-20821`](https://youtrack.jetbrains.com/issue/KT-20821) Error while inlining function reference implicitly applied to this
- [`KT-21299`](https://youtrack.jetbrains.com/issue/KT-21299) Restore adding JDK roots to the beginning of the classpath list

### IDE

- [`KT-21180`](https://youtrack.jetbrains.com/issue/KT-21180) Project level api/language version settings are erroneously used as default during Gradle import
- [`KT-21335`](https://youtrack.jetbrains.com/issue/KT-21335) Fix exception on Project Structure view open
- [`KT-21610`](https://youtrack.jetbrains.com/issue/KT-21610) Fix "Could not determine the class-path for interface KotlinGradleModel" on Gradle sync
- Optimize dependency handling during import of Gradle project

### JavaScript

- [`KT-21493`](https://youtrack.jetbrains.com/issue/KT-21493) Losing lambda defined in inline function after incremental recompilation

### Tools. CLI

- [`KT-21495`](https://youtrack.jetbrains.com/issue/KT-21537) Bash scripts in Kotlin v1.2 compiler have Windows line terminators 
- [`KT-21537`](https://youtrack.jetbrains.com/issue/KT-21537) javac 7 do nothing when kotlin-compiler(-embeddable) is in classpath

### Libraries

- Unify docs wording of 'trim*' functions 
- Improve cover documentation page of kotlin.test library 
- Provide summary for kotlin.math package 
- Fix unresolved references in the api docs 

## Previous releases

This release also includes fixes and improvements from previous release:

- [`v1.2.0`](https://github.com/JetBrains/kotlin/releases/tag/v1.2.0)
