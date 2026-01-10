// FILE: lib.kt
class A {
    companion object {
        val s = "OK"
        var v = "NOT OK"
    }

    inline fun f(): String = s

    inline fun g() {
        v = "OK"
    }
}

// FILE: main.kt
fun box(): String {
    val a = A()
    if (a.f() != "OK") return "FAIL0"
    a.g()
    return A.v
}