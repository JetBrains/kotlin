fun foo(o: Any?): String {
    return when (o) {
        is Int -> "Fail"
        null -> "OK"
        else -> "Fail_default"
    }
}

fun box(): String {
    return foo(null)
}