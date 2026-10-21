// IGNORE_STANDALONE
// Standalone mode doesn't provide PSI for declarations from binary libraries
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

class Outer<A> {
    inner class Inner<B>
}

fun <X, Y> foo(p: Outer<X>.Inner<Y>, q: List<Outer<List<X>>.Inner<Map<X, Y>>>): Unit = TODO()

// MODULE: main(lib)
// FILE: main.kt
import lib.foo

fun test() {
    f<caret>oo<Int, String>(TODO(), TODO())
}
