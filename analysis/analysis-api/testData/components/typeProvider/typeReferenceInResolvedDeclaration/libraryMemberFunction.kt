// IGNORE_STANDALONE
// Standalone mode doesn't provide PSI for declarations from binary libraries
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

class Box<T> {
    fun <R> map(f: (T) -> R): Box<Map<T, R>> = TODO()
}

// MODULE: main(lib)
// FILE: main.kt
import lib.Box

fun test(box: Box<Int>) {
    box.m<caret>ap { it.toString() }
}
