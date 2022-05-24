// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-52407

// FILE: x.kt

package x

class Throws {
    fun test() {}
}

// FILE: main.kt

import x.*

fun main() {
    Throws().test()
}