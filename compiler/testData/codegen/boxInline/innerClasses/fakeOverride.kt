// FILE: 1.kt

open class C {
    fun f() = "OK"
}

inline fun inlineFun(): String {
    val cc = object : C() {}
    return cc.f()
}

// FILE: 2.kt
fun box() = inlineFun()