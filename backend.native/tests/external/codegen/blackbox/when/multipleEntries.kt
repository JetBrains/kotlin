fun foo(x: Any) =
        when (x) {
            0, 1 -> "bit"
            else -> "something"
        }

fun box(): String {
    if (foo(0) != "bit") return "Fail 0"
    if (foo(1) != "bit") return "Fail 1"
    if (foo(2) != "something") return "Fail 2"
    return "OK"
}
