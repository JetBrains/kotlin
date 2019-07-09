// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val progression = 1 .. 3 step 2

fun box(): String = when {
    0 in progression -> "fail 1"
    1 !in progression -> "fail 2"
    else -> "OK"
}