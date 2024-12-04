// SKIP_UNBOUND_IR_SERIALIZATION
// ^^^ Muted until KT-72296 is fixed.

// FILE: 1.kt
open class C {
    fun o() = "O"
    val k = "K"
}

inline fun inlineFun(): String {
    val cc = object : C() {}
    return cc.o() + cc.k
}

// FILE: 2.kt
fun box() = inlineFun()