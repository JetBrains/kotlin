// WITH_STDLIB

val arr = intArrayOf()

fun box(): String {
    val s = StringBuilder()
    for ((index, x) in arr.withIndex()) {
        return "Loop over empty array should not be executed"
    }
    return "OK"
}