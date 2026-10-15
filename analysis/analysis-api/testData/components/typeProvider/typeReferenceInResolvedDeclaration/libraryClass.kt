// IGNORE_STANDALONE
// Standalone mode doesn't provide PSI for declarations from binary libraries
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

annotation class Anno

open class Base<T>

@Anno
class Foo<T>(@Anno val p: T, q: List<T>) : Base<T>(), Comparable<Foo<T>> where T : CharSequence, T : Comparable<T> {
    override fun compareTo(other: Foo<T>): Int = 0
}

// MODULE: main(lib)
// FILE: main.kt
import lib.Foo

fun test(foo: F<caret>oo<String>) {}
