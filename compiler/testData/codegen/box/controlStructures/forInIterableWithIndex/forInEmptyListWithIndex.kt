// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val xs = listOf<Any>()

fun box(): String {
    val s = StringBuilder()
    for ((index, x) in xs.withIndex()) {
        return "Loop over empty list should not be executed"
    }
    return "OK"
}