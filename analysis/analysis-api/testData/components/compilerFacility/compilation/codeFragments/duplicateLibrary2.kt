// MODULE: lib1
// MODULE_KIND: LibraryBinary
// FILE: lib1.kt
package lib
fun foo(a: String): Int = 0
fun foo(n: Int): Int = 1

// MODULE: lib2
// MODULE_KIND: LibraryBinary
// FILE: lib2.kt
package lib
fun foo(a: String): Int = 0
fun foo(n: Char): Int = 1

// MODULE: lib3
// MODULE_KIND: LibraryBinary
// FILE: lib3.kt
package lib
fun foo(n: Int): Int = 0
fun foo(n: Char): Int = 1

// MODULE: context(lib1, lib2, lib3)
// FILE: context.kt
import lib.*

fun test() {
    <caret_context>val x = 0
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context
foo("x") + foo(1) + foo('c')