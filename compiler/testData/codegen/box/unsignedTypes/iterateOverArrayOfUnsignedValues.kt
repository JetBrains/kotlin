// WITH_UNSIGNED
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR

fun box(): String {
    var sum = 0u
    for (el in arrayOf(1u, 2u, 3u)) {
        sum += el
    }

    if (sum != 6u) return "Fail 1"

    sum = 0u
    for (el in uintArrayOf(10u, 20u)) {
        sum += el
    }

    if (sum != 30u) return "Fail 2"

    return "OK"
}