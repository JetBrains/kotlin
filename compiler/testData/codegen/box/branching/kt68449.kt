// WITH_STDLIB
fun func(obj: Any): String {
    val result = when (obj) {
        is String -> obj.uppercase()
        is Long -> obj + 10
        else -> {}
    }
    return result.toString()
}

fun box(): String {
    return func("ok")
}