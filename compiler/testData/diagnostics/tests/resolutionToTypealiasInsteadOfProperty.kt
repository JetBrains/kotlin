// FIR_IDENTICAL
// ISSUE: KT-58523

// FILE: pkg.kt

package pkg

class Klass
typealias ItemKey = Klass

// FILE: main.kt

import pkg.ItemKey

val ItemKey = 42

fun main() {
    ItemKey // K1: ok, K2: NO_COMPANION_OBJECT
}
