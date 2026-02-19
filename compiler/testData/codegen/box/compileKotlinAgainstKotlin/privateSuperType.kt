// MODULE: lib
// FILE: Z.kt
package z

private interface I {
     fun k() = "K"
}

interface G<T>

class Z : I, G<I> {
    val o = "O"
}

// MODULE: main(lib)
// FILE: box.kt
import z.Z

fun box() = Z().run { o + k() }