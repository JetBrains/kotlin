// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

fun box() : String {
    val value = (1 to doubleArrayOf(1.0)).second[0]
    return if (value == 1.0) "OK" else "fail"
}
