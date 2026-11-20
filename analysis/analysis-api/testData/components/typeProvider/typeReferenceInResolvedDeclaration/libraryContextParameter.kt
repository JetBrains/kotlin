// IGNORE_STANDALONE
// Standalone mode doesn't provide PSI for declarations from binary libraries
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

context(c: List<T>)
fun <T> T.foo(): T = TODO()

// MODULE: main(lib)
// FILE: main.kt
import lib.foo

fun test() {
    with(listOf(1)) {
        1.f<caret>oo()
    }
}
