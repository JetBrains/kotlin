// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
@file:Anno
package org.example

@Target(AnnotationTarget.FILE)
annotation class Anno

fun other() {}

// MODULE: main(lib)
// FILE: main.kt
import org.example.*

fun main() {
    other()
}
