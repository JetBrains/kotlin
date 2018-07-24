# CHANGELOG

## 1.3-M1

### Language design

- [`KEEP-104`](https://github.com/Kotlin/KEEP/issues/104) Inline classes
- [`KEEP-135`](https://github.com/Kotlin/KEEP/issues/135) Unsigned integer types
- [`KEEP-95`](https://github.com/Kotlin/KEEP/issues/95) Experimental API annotations
- [`KT-13762`](https://youtrack.jetbrains.com/issue/KT-13762) Prohibit annotations with target 'EXPRESSION' and retention 'BINARY' or 'RUNTIME'
- [`KT-16681`](https://youtrack.jetbrains.com/issue/KT-16681) kotlin allows mutating the field of read-only property
- [`KT-21515`](https://youtrack.jetbrains.com/issue/KT-21515) Restrict visibility of classifiers, nested in companion objects

### Compiler

- [`KT-24848`](https://youtrack.jetbrains.com/issue/KT-24848) Refine loading Java overrides of Kotlin suspend functions
- [`KT-9580`](https://youtrack.jetbrains.com/issue/KT-9580) Report an error if 'setparam' target does not make sense for a parameter declaration
- [`KT-16310`](https://youtrack.jetbrains.com/issue/KT-16310) Nested classes inside enum entries capturing outer members
- [`KT-17981`](https://youtrack.jetbrains.com/issue/KT-17981) Type parameter for catch parameter possible when exception is nested in generic, but fails in runtime
- [`KT-21354`](https://youtrack.jetbrains.com/issue/KT-21354) Inconsistent behavior of 'for-in-range' loop if range is an array variable modified in loop body
- [`KT-25333`](https://youtrack.jetbrains.com/issue/KT-25333) Restrict visibility of Java static members from supertypes of companion object
- [`KT-25623`](https://youtrack.jetbrains.com/issue/KT-25623) Do not load experimental coroutines as non-suspend function with additional Continuation parameter

#### Backend. JVM

- [`KT-6301`](https://youtrack.jetbrains.com/issue/KT-6301) Support JvmStatic annotation on interface companion object members
- [`KT-25508`](https://youtrack.jetbrains.com/issue/KT-25508) Inject probeCoroutineSuspended to coroutines body
- [`KT-18987`](https://youtrack.jetbrains.com/issue/KT-18987) Unroll recursion in CoroutineImpl.resume
- [`KT-11567`](https://youtrack.jetbrains.com/issue/KT-11567) Companion object INSTANCE field more visible than companion object class itself
- [`KT-13764`](https://youtrack.jetbrains.com/issue/KT-13764) Support lambdas and function references for arities bigger than 22
- [`KT-16615`](https://youtrack.jetbrains.com/issue/KT-16615) Do not generate ConstantValue attribute for non-const vals
- [`KT-25193`](https://youtrack.jetbrains.com/issue/KT-25193) Names of parameters from Java interface methods implemented by delegation are lost
- [`KT-25324`](https://youtrack.jetbrains.com/issue/KT-25324) VerifyError “Bad type on operand stack” on running code with call of array iterator for array of inline classes
- [`KT-25325`](https://youtrack.jetbrains.com/issue/KT-25325) CCE “Foo cannot be cast to java.lang.String” for iterating over the list of inline classes
- [`KT-25626`](https://youtrack.jetbrains.com/issue/KT-25626) Inline class values inside string literals don't use their own toString implementation

#### Backend. JVM. Coroutines

- [`KT-20219`](https://youtrack.jetbrains.com/issue/KT-20219) Inline suspend function can't be used as non-inline function
- [`KT-24863`](https://youtrack.jetbrains.com/issue/KT-24863) Support new Continuation API in JVM BE
- [`KT-24864`](https://youtrack.jetbrains.com/issue/KT-24864) Create new CoroutineImpl and other internal coroutines interfaces

#### Backend. JVM. Inline

- [`KT-25511`](https://youtrack.jetbrains.com/issue/KT-25511) Inline classes fail with cross-inline functions

#### Binary Metadata

- [`KT-24617`](https://youtrack.jetbrains.com/issue/KT-24617) Optional expected annotation is unresolved in a dependent platform module
- [`KT-25120`](https://youtrack.jetbrains.com/issue/KT-25120) RequireKotlin on nested class and its members is not loaded correctly
- [`KT-25273`](https://youtrack.jetbrains.com/issue/KT-25273) java.lang.UnsupportedOperationException from incremental JS compilation
- [`KT-25310`](https://youtrack.jetbrains.com/issue/KT-25310) Write isUnsigned flag into metadata for unsigned types


#### Frontend

- [`KT-15807`](https://youtrack.jetbrains.com/issue/KT-15807) @JvmField is not applicable to interface companion properties
- [`KT-16962`](https://youtrack.jetbrains.com/issue/KT-16962) Annotation classes cannot contain types or static fields because they cannot have a body
- [`KT-23153`](https://youtrack.jetbrains.com/issue/KT-23153) Compiler allows to set non constant value as annotation parameter
- [`KT-23362`](https://youtrack.jetbrains.com/issue/KT-23362) Move coroutines to package kolin.coroutines for 1.3
- [`KT-24861`](https://youtrack.jetbrains.com/issue/KT-24861) Fix loading kotlin.suspend compiled with LV=1.2 when release coroutines package is used
- [`KT-25241`](https://youtrack.jetbrains.com/issue/KT-25241) Kotlin compiler doesn't warn about usage of lambda/reference with more than 22 parameters
- [`KT-25600`](https://youtrack.jetbrains.com/issue/KT-25600) NSEE “Collection is empty” after trying to call default constructor of unsigned number class

#### Frontend. Data-flow analysis

- [`KT-22517`](https://youtrack.jetbrains.com/issue/KT-22517) Deprecate smartcasts for local delegated properties

#### Frontend. Declarations

- [`KT-19618`](https://youtrack.jetbrains.com/issue/KT-19618) Data class `copy()` call with optional parameters leads to broken code when the class implements an interface with `copy()`
- [`KT-23277`](https://youtrack.jetbrains.com/issue/KT-23277) Prohibit local annotation classes
- [`KT-24197`](https://youtrack.jetbrains.com/issue/KT-24197) Make 'mod' operator error in 1.3

#### Frontend. Lexer & Parser

- [`KT-24663`](https://youtrack.jetbrains.com/issue/KT-24663) Add 'UL' suffix to represent number literals of unsigned Long type

#### Frontend. Resolution and Inference

- [`KT-16908`](https://youtrack.jetbrains.com/issue/KT-16908) Support callable references to suspending functions
- [`KT-20588`](https://youtrack.jetbrains.com/issue/KT-20588) Report error on single element assignment to varargs in named form in annotations
- [`KT-20589`](https://youtrack.jetbrains.com/issue/KT-20589) Report error on single element assignment to varargs in named form in functions
- [`KT-24859`](https://youtrack.jetbrains.com/issue/KT-24859) Disallow calls of functions annotated with receiver annotated with @RestrictsSuspension in foreign suspension context

### IDE

- [`KT-25466`](https://youtrack.jetbrains.com/issue/KT-25466) Make coroutines resolve to be independent of language and API versions set in `Kotlin Compiler` settings

#### IDE. Inspections and Intentions

- [`KT-24243`](https://youtrack.jetbrains.com/issue/KT-24243) Support quick fix to enable usages of Experimental/UseExperimental
- [`KT-11154`](https://youtrack.jetbrains.com/issue/KT-11154) Spell checking inspection is not suppressable
- [`KT-25169`](https://youtrack.jetbrains.com/issue/KT-25169) Impossible to suppress UAST/JVM inspections

#### IDE. Libraries

- [`KT-25129`](https://youtrack.jetbrains.com/issue/KT-25129) Idea freezes when Kotlin plugin tries to determine if jar is js lib in jvm module


### Libraries

#### New Features

- [`KEEP-131`](https://github.com/Kotlin/KEEP/issues/131), [`KT-17261`](https://youtrack.jetbrains.com/issue/KT-17261) Add random number generator to stdlib and related collection extension functions
- [`KT-7922`](https://youtrack.jetbrains.com/issue/KT-7922) Companion object for Boolean
- [`KT-8247`](https://youtrack.jetbrains.com/issue/KT-8247) Byte size for primitives
- [`KT-16552`](https://youtrack.jetbrains.com/issue/KT-16552) Add Sequence.orEmpty()
- [`KT-18559`](https://youtrack.jetbrains.com/issue/KT-18559) Make SafeContinuation and context impl classes serializable
- [`KT-18910`](https://youtrack.jetbrains.com/issue/KT-18910) StringBuilder#setLength(0) in non-JVM Kotlin
- [`KT-21763`](https://youtrack.jetbrains.com/issue/KT-21763) Provide Char MIN_VALUE/MAX_VALUE constants
- [`KT-23279`](https://youtrack.jetbrains.com/issue/KT-23279) isNullOrEmpty() for collections, maps, and arrays
- [`KT-23602`](https://youtrack.jetbrains.com/issue/KT-23602) Make kotlin.Metadata public

#### Fixes

- [`KT-23564`](https://youtrack.jetbrains.com/issue/KT-23564) KotlinJS: Math is deprecated yet random is not a global function
- [`KT-24856`](https://youtrack.jetbrains.com/issue/KT-24856) Create compatibility wrappers for migration from 1.2 to 1.3 coroutines
- [`KT-24862`](https://youtrack.jetbrains.com/issue/KT-24862) Create new Continuation API for coroutines

### Reflection

- [`KT-25541`](https://youtrack.jetbrains.com/issue/KT-25541) Incorrect parameter names in reflection for inner class constructor from Java class compiled with "-parameters"


## Previous releases

This release also includes the fixes and improvements from the
[`1.2.60`](https://github.com/JetBrains/kotlin/blob/1.2.60/ChangeLog.md) release.
