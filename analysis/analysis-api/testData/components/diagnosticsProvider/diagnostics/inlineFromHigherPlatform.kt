// MODULE: lib
// MODULE_KIND: LibraryBinary
// JVM_TARGET: 11
// FILE: lib.kt
package lib

inline fun inlineFunction(block: () -> Unit) {
    block()
}

fun regularFunction() {}

// MODULE: main(lib)
// JVM_TARGET: 1.8
// FILE: main.kt
package main

import lib.*

// The check needs the class file version of the library declaration. Stub-based deserialization of libraries doesn't provide it.
fun usage() {
    inlineFunction {}
    regularFunction()
}
