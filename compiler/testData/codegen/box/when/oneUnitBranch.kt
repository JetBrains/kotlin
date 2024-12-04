// ISSUE: KT-68449

fun foo(x: Any): String {
    val result = when (x) {
        is String -> x.toString()
        is Long -> x + 10
        else -> {}
    }
    return result.toString()
}

fun box(): String {
    return foo("OK")
}
