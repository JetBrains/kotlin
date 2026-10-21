// IGNORE_STANDALONE
// Standalone mode doesn't provide PSI for declarations from binary libraries
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

fun <T> foo(p: T & Any, q: List<T & Any>): T & Any = TODO()

// MODULE: main(lib)
// FILE: main.kt
import lib.foo

fun test() {
    f<caret>oo<Int?>(1, emptyList())
}
