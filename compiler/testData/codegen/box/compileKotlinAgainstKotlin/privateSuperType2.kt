// MODULE: lib
// FILE: Z.kt
package z

private interface I {
     fun k() = "K"
}

private interface II : I

class Z : II {
    val o = "O"
}

// MODULE: main(lib)
// FILE: box.kt
import z.Z

fun box() = Z().run { o + k() }