// MODULE: lib
// MODULE_KIND: LibraryBinary
// JVM_TARGET: 11
// FILE: lib.kt
package lib

inline fun inlineFunction(block: () -> Unit) {
    block()
}

fun regularFunction() {}

inline val inlineProperty: Int
    get() = 1

var propertyWithInlineSetter: Int
    get() = 1
    inline set(value) {}

// MODULE: main(lib)
// JVM_TARGET: 1.8
// FILE: main.kt
package main

import lib.*

fun usage() {
    inlineFunction {}
    regularFunction()
    inlineProperty
    propertyWithInlineSetter
    propertyWithInlineSetter = 2
}
