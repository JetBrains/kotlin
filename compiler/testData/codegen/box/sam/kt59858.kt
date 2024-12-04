// MODULE: A
// FILE: A.kt
interface Base {
    fun f() : Any
}

fun interface Child : Base {
    override fun f() : Int
}

// MODULE: B(A)
// FILE: B.kt
fun box(): String {
    if (Child { 10 }.f() != 10) return "FAIL"
    return "OK"
}
