const val byte10: Byte = 10
const val short50: Short = 50

fun foo(p: Any): String {
    return when (p) {
        0 -> "0"
        byte10 -> "byte10"
        short50 -> "short50"
        'z' -> "z"
        else -> "else"
    }
}

fun box(): String {
    return if (foo(0) == "0"
        && foo(byte10) == "byte10"
        && foo(short50) == "short50"
        && foo('z') == "z"
        && foo("else") == "else"
    ) "OK"
    else "FAIL"
}