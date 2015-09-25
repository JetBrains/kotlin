interface A {
    fun foo() = "OK"
}

fun box(): String {
    val result = B.test(object : A {})
    if (result != "OK") return "fail: $result"

    return "OK"
}
