// NO_CHECK_LAMBDA_INLINING

// MODULE: lib
// FILE: lib.kt
package lib

open class C<T> {
    fun tostr(c: T) = c.toString()
    val k = "K"
}

inline fun inlineFun(): String {
    val cc = object : C<Char>() {}
    return (cc::tostr)('O') + cc::k.get()
}

// MODULE: main(lib)
// FILE: box.kt
fun box() = lib.inlineFun()