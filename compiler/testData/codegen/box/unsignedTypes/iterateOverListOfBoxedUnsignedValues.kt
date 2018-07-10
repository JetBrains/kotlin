// WITH_UNSIGNED
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR

fun box(): String {
    var sum = 0u
    val ls = listOf(1u, 2u, 3u)
    for (el in ls) {
        sum += el
    }

    return if (sum != 6u) "Fail" else "OK"
}