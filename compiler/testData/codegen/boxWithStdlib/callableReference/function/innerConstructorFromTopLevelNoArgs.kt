class A {
    inner class Inner {
        val o = 111
        val k = 222
    }
}

fun box(): String {
    val result = (::A)().(A::Inner)().o + A().(A::Inner)().k
    if (result != 333) return "Fail $result"
    return "OK"
}
