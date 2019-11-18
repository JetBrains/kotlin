// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

// See https://youtrack.jetbrains.com/issue/KT-21354

fun box(): String {
    var xs = intArrayOf(1, 2, 3)
    var sum = 0
    for (x in xs) {
        sum = sum * 10 + x
        xs = intArrayOf(4, 5)
    }
    return if (sum == 123) "OK" else "Fail: $sum"
}
