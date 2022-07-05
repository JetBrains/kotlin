// NO_CHECK_LAMBDA_INLINING
// MODULE: lib
// FILE: lib.kt
package lib

open class C {
    fun o() = "O"
    val k = "K"
}

inline fun inlineFun(): String {
    val cc = object : C() {}
    return cc.o() + cc.k
}

// MODULE: main(lib)
// FILE: box.kt
fun box() = lib.inlineFun()