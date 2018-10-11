// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME
// LANGUAGE_VERSION: 1.2
// IGNORE_BACKEND: JS

fun box(): String {
    var xs = intArrayOf(1, 2, 3)
    var sum = 0
    for (x in xs) {
        sum = sum * 10 + x
        xs = intArrayOf(4, 5)
    }
    return if (sum == 15) "OK" else "Fail: $sum"
}