// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val array = arrayOf(doubleArrayOf(-1.0))
    for (node in array) {
        node[0] += 1.0
    }
    if (array[0][0] != 0.0) return "fail ${array[0][0]}"

    return "OK"
}