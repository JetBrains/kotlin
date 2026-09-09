// constructor: one/Foo.init
// ISSUE: KT-64686
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

object Foo

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
