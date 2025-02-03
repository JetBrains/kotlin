// class: one/Foo
// LANGUAGE: +ContextReceivers
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

context(Int, String)
class Foo

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
