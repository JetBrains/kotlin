class A {
    fun o() = 111
    fun k(k: Int) = k
}

fun A.foo() = this.(::o)() + this.(A::k)(222)

fun box(): String {
    val result = A().foo()
    if (result != 333) return "Fail $result"
    return "OK"
}
