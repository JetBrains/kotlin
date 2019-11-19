fun foo(s: Any): String {
    val x = when (s) {
        is String -> s
        is Int -> "$s"
        else -> return ""
    }

    val y: String = x
    return y
}

fun box() = if (foo("OK") == "OK" && foo(42) == "42" && foo(true) == "") "OK" else "Fail"
