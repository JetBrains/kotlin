// ISSUE: KT-68727
// MODULE: lib
// FILE: lib.kt

package lib

class Enum(
    val placeholder: String = "OK"
) {
    override fun toString(): String = placeholder
}

// MODULE: main(lib)
// FILE: main.kt

import lib.Enum

fun box(): String {
    val x = Enum()
    return x.toString()
}
