// IGNORE_BACKEND_FIR: JVM_IR
inline fun <reified T> jaggedArray(x: (Int, Int, Int) -> T): Array<Array<Array<T>>> = Array(1) { i ->
    Array(1) {
        j -> Array(1) { k -> x(i, j, k)  }
    }
}

fun box(): String {
    val x1: Array<Array<Array<String>>> = jaggedArray<String>() { x, y, z -> "$x-$y-$z" }
    if (x1[0][0][0] != "0-0-0") return "fail 1"

    val x2: Array<Array<Array<Array<String>>>> = jaggedArray() { x, y, z -> arrayOf("$x-$y-$z") }
    if (x2[0][0][0][0] != "0-0-0") return "fail 2"

    val x3: Array<Array<Array<IntArray>>> = jaggedArray() { x, y, z -> intArrayOf(x + y + z + 1) }
    if (x3[0][0][0][0] != 1) return "fail 3"
    return "OK"
}
