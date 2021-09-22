// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val arr = arrayOf("a", "b", "c", "d")

fun box(): String {
    var count = 0

    for ((_, _) in arr.withIndex()) {
        count++
    }

    return if (count == 4) "OK" else "fail: '$count'"
}