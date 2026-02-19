// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

@Deprecated("Use 'bar() instead", replaceWith = ReplaceWith("bar()"))
fun foo() {}

fun bar() {}

// MODULE: main(lib)
// FILE: main.kt
import lib.*

fun test() {
    f<caret>oo()
}