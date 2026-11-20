// IGNORE_STANDALONE
// Standalone mode doesn't provide PSI for declarations from binary libraries
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

typealias MapList<T> = List<Map<T, List<T>>>

// MODULE: main(lib)
// FILE: main.kt
import lib.MapList

fun test(p: Map<caret>List<Int>) {}
