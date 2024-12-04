class A {
    inner class Inner {
        val o = 111
        val k = 222
    }
}

fun box(): String {
    val result = (A::Inner).let { c -> c((::A).let { it() }).o } + (A::Inner).let { it(A()) }.k
    if (result != 333) return "Fail $result"
    return "OK"
}
