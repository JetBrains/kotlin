fun ok(): String = "OK"

fun callFirst(): String = (::ok)()

fun callSecond(): String {
    val f: () -> String = ::ok
    return f()
}

fun box(): String {
    val result = callFirst() + callSecond()
    return if (result == "OKOK") "OK" else "FAIL: $result"
}
