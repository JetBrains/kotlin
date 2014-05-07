class A {
    inner class Inner(val result: Int)
}

fun box(): String {
    val result = (::A)().(A::Inner)(111).result + A().(A::Inner)(222).result
    if (result != 333) return "Fail $result"
    return "OK"
}
