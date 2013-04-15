class A {
    inner class Inner {
        val o = 111
        val k = 222
    }
}

fun A.foo() = this.(A::Inner)().o + this.(::Inner)().k

fun box(): String {
    val result = A().foo()
    if (result != 333) return "Fail $result"
    return "OK"
}
