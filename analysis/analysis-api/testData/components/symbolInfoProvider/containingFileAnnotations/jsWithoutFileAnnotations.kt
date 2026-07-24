// MODULE: library
// TARGET_PLATFORM: JS
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package test

fun foo() = 42

// MODULE: main(library)
// FILE: main.kt
fun test() {
    foo()
}
