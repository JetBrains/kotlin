// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

val iterable: Iterable<Int> = listOf(1, 2, 3)

fun box(): String = when {
    0 in iterable -> "fail 1"
    1 !in iterable -> "fail 2"
    else -> "OK"
}