// IGNORE_FE10

// MODULE: lib
// LANGUAGE: +ExportKDocDocumentationToKlib
// TARGET_PLATFORM: JS
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