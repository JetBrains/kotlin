// WITH_STDLIB

val range = 1 .. 3

fun box(): String = when {
    0 in range -> "fail 1"
    1 !in range -> "fail 2"
    else -> "OK"
}