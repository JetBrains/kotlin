// NO_CHECK_LAMBDA_INLINING
// SKIP_UNBOUND_IR_SERIALIZATION
// ^^^ Muted until KT-72296 is fixed.

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