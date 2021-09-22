// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val xs = "abcd"

fun box(): String {
    var count = 0

    for ((_, _) in xs.withIndex()) {
        count++
    }

    return if (count == 4) "OK" else "fail: '$count'"
}