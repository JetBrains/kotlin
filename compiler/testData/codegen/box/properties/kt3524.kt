val i: Any = 12

fun box(): String {
    return if (i == 12) "OK" else "fail"
}