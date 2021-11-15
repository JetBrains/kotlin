// WITH_STDLIB
// IGNORE_BACKEND: JVM, JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

fun <T : IntArray> test(array: T): Int {
    var sum = 0
    for (i in array.indices) {
        sum = sum * 10 + i
    }
    return sum
}

fun box(): String {
    if (test(intArrayOf(0, 0, 0, 0)) != 123) return "fail"
    return "OK"
}