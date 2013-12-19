fun box(): String {
    if (!foo(1.toByte())) return "fail 1"
    if (!foo((1.toByte()).inc())) return "fail 2"

    return "OK"
}

fun foo(p: Any) = p is Byte