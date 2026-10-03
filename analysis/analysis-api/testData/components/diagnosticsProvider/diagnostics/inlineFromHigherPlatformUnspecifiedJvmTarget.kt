// MODULE: lib
// MODULE_KIND: LibraryBinary
// JVM_TARGET: 11
// FILE: lib.kt
package lib

inline fun inlineFunction(block: () -> Unit) {
    block()
}

// MODULE: main(lib)
// TARGET_PLATFORM: JVM
// FILE: main.kt
package main

import lib.*

// The JVM target of `main` is unknown, so `INLINE_FROM_HIGHER_PLATFORM` cannot be reported.
fun usage() {
    inlineFunction {}
}
