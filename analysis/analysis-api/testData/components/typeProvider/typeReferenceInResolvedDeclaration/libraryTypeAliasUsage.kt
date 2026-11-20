// IGNORE_STANDALONE
// Standalone mode doesn't provide PSI for declarations from binary libraries
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

typealias StringMap<V> = Map<String, V>

fun <T> foo(p: StringMap<T>, q: List<StringMap<List<T>>>): Unit = TODO()

// MODULE: main(lib)
// FILE: main.kt
import lib.foo

fun test() {
    f<caret>oo<Int>(emptyMap(), emptyList())
}
