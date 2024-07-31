// MODULE: lib1
// MODULE_KIND: LibraryBinary
// FILE: lib1.kt
package lib
fun foo(n: String): Int = 1

// MODULE: lib2
// MODULE_KIND: LibraryBinary
// FILE: lib2.kt
package lib
fun foo(a: Int): Int = 2

// MODULE: lib3
// MODULE_KIND: LibraryBinary
// FILE: lib3.kt
package lib
fun foo(n: Char): Int = 3

// MODULE: main(lib1, lib2, lib3)
// FILE: main.kt
import lib.*

fun test() {
    <caret>val x = 0
}