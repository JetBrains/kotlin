// NO_CHECK_LAMBDA_INLINING
// MODULE: lib
// FILE: lib.kt
package lib

open class C {
    fun o() = "O"
    val k = "K"
}

inline fun inlineFun(f: () -> String = { val cc = object : C() {}; cc.o() + cc.k; }): String {
    return f()
}

// MODULE: main(lib)
// FILE: box.kt
fun box() = lib.inlineFun()