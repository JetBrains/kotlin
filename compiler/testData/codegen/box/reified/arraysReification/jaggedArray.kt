// IGNORE_BACKEND_FIR: JVM_IR
inline fun <reified T> jaggedArray(x: (Int, Int) -> T): Array<Array<T>> = Array(1) { i ->
    Array(1) { j -> x(i, j) }
}

fun box(): String {
    val x1: Array<Array<String>> = jaggedArray<String>() { x, y -> "$x-$y" }
    if (x1[0][0] != "0-0") return "fail 1"

    val x2: Array<Array<Array<String>>> = jaggedArray() { x, y -> arrayOf("$x-$y") }
    if (x2[0][0][0] != "0-0") return "fail 2"

    val x3: Array<Array<IntArray>> = jaggedArray() { x, y -> intArrayOf(x + y + 1) }
    if (x3[0][0][0] != 1) return "fail 3"
    return "OK"
}
