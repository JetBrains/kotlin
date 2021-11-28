class A(var x: String)

fun f(s: String): String {
    fun A.localX() {
        x = s + "K"
    }

    val a: A = A("FAIL")
    a.apply(A::localX)
    if (a.x != "OK") return a.x
    a.apply { localX() }
    return a.x
}

fun box(): String {
    return f("O")
}
