// IGNORE_FE10
// KT-70384

// In this test, we have two modules `module1` and `module2` which each depend on a different version of a library which contains a class
// called `library.Common`. These two modules are entirely independent, so `module1` should see the class from `library1` and `module2`
// should see the class from `library2` without any interference or unresolved symbols.

// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: library1.kt
package library

class Common {
    fun foo(): String = "common"
}

// MODULE: library2
// MODULE_KIND: LibraryBinary
// FILE: library2.kt
package library

class Common {
    fun bar(): Int = 5
}

// MODULE: module1(library1)
// FILE: module1.kt
import library.Common

val common: Common = Common()

val value = common.foo()

// MODULE: module2(library2)
// FILE: module2.kt
import library.Common

val common: Common = Common()

val value = common.bar()
