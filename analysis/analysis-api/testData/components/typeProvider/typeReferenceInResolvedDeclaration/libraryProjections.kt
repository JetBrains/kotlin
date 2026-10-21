// IGNORE_STANDALONE
// Standalone mode doesn't provide PSI for declarations from binary libraries
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

fun <T> foo(p: Map<*, List<T>>, q: Comparable<in T>, r: Array<out T>): Unit = TODO()

// MODULE: main(lib)
// FILE: main.kt
import lib.foo

fun test() {
    f<caret>oo<Int>(emptyMap(), TODO(), emptyArray())
}
