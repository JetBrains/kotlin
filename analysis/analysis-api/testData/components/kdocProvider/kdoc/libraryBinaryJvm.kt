// IGNORE_FE10

// MODULE: lib
// TARGET_PLATFORM: JVM
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

/**
 * This is a sample function.
 */
fun foo() {}

// MODULE: main(lib)
// FILE: main.kt
import lib.foo

fun test() {
    foo()
}