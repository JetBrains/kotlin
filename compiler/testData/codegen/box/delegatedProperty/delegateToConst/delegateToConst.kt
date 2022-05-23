// WITH_STDLIB

operator fun Any?.getValue(x: Any?, y: Any?): String {
    return "OK"
}

val s: String by 1

fun box() = s